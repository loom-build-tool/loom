/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.plugin.eclipse;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import builders.loom.api.AbstractTask;
import builders.loom.api.JavaVersion;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;
import builders.loom.util.xml.XmlBuilder;
import builders.loom.util.xml.XmlUtil;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class EclipseModuleTask extends AbstractTask implements ModuleGraphAware {
    private final DocumentBuilder docBuilder;

    private final Transformer transformer;
    private Map<Module, Set<Module>> moduleGraph;

    public EclipseModuleTask() {
        try {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            docBuilder = dbFactory.newDocumentBuilder();

            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        } catch (final ParserConfigurationException | TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> transitiveModuleGraph) {
        this.moduleGraph = transitiveModuleGraph;
    }

    @Override
    public TaskResult run() throws Exception {

        final Path currentDir = Paths.get("");

        final Path currentWorkDirName = currentDir.toAbsolutePath().getFileName();

        if (currentWorkDirName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        for (final Module module : allModules()) {
            createModuleProject(module);
        }

        return completeOk(new DummyProduct("Eclipse project files"));
    }

    private Set<Module> allModules() {
        return moduleGraph.keySet();
    }

    private void createModuleProject(final Module module)
        throws IOException, SAXException, TransformerException, InterruptedException {

        final Path projectXml = module.getPath().resolve(".project");

        if (Files.notExists(projectXml)) {
            writeDocumentToFile(projectXml, createProjectFile(module));
        } else {
            final Document projectXmlDoc = docBuilder.parse(projectXml.toFile());
            if (mergeProjectBuildSpec(projectXmlDoc) || mergeProjectNature(projectXmlDoc)) {
                writeDocumentToFile(projectXml, projectXmlDoc);
            }
        }
        writeDocumentToFile(module.getPath().resolve(".classpath"), createClasspathFile(module));
        final Path settingsPath = Files.createDirectories(module.getPath().resolve(".settings"));
        writePropertiesToFile(settingsPath.resolve("org.eclipse.jdt.core.prefs"),
            createJdtPrefs(module));
    }

    private Properties createJdtPrefs(final Module module) {

        final Properties prefs = new Properties();
        final String javaLangLevel =
            buildProjectJdkName(module.getConfig().getBuildSettings().getJavaPlatformVersion());

        prefs.setProperty("eclipse.preferences.version", "1");
        prefs.setProperty("org.eclipse.jdt.core.compiler.source", javaLangLevel);
        prefs.setProperty("org.eclipse.jdt.core.compiler.compliance", javaLangLevel);

        return prefs;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static String buildProjectJdkName(final JavaVersion javaVersion) {
        final int numericVersion = javaVersion.getNumericVersion();
        return (numericVersion < 9) ? "1." + numericVersion : String.valueOf(numericVersion);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createProjectFile(final Module module) {

        return XmlBuilder.root("projectDescription")
            .element("name").text(module.getModuleName())
                .and()
            .element("comment")
                .and()
            .element("projects")
                .and()
            .element("natures")
                .element("nature").text("org.eclipse.jdt.core.javanature")
                    .and()
                .and()
            .element("buildSpec")
                .element("buildCommand")
                    .element("name").text("org.eclipse.jdt.core.javabuilder")
                        .and()
                    .element("arguments")
                        .and()
                    .and()
                .and()
            .element("linkedResources")
                .and()
            .element("filteredResources")
            .getDocument();

    }

    private boolean mergeProjectBuildSpec(final Document projectXml) {

        boolean found = false;

        final Element buildSpec = XmlUtil.getOnlyElement(projectXml.getElementsByTagName("buildSpec"));

        for(final Node itemX : XmlUtil.iterable(projectXml.getElementsByTagName("buildCommand"))) {

            // FIXME
            Element item = (Element ) itemX;


            found |= XmlUtil.getOnlyElement(
                item.getElementsByTagName("name")).getTextContent()
                .equals("org.eclipse.jdt.core.javabuilder");
        }

        if (!found) {
            XmlBuilder.wrap(buildSpec)
                .element("buildCommand")
                    .element("name").text("org.eclipse.jdt.core.javabuilder")
                        .and()
                    .element("arguments");
            return true;
        }

        return false;
    }

    private boolean mergeProjectNature(final Document projectXml) {

        boolean found = false;

        final Element naturesNode = XmlUtil.getOnlyElement(projectXml.getElementsByTagName("natures"));

        for (final Node item : XmlUtil.iterable(projectXml.getElementsByTagName("nature"))) {
            
            found |= item.getTextContent().equals("org.eclipse.jdt.core.javanature");

        }

        if (!found) {
            XmlBuilder.wrap(naturesNode)
                .element("nature")
                .text("org.eclipse.jdt.core.javanature");
            return true;
        }

        return false;
    }

    private void writeDocumentToFile(final Path file, final Document doc)
        throws IOException, TransformerException {

        try (final OutputStream outputStream = newOut(file)) {
            doc.setXmlStandalone(true);

            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        }
    }

    private void writePropertiesToFile(final Path file, final Properties properties)
        throws IOException {

        try (final OutputStream outputStream = newOut(file)) {
            properties.store(outputStream, "Loom Eclipse Plugin");
        }

    }
    // TODO use XmlWriter

    private OutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createClasspathFile(final Module module)
        throws InterruptedException {

        final XmlBuilder.Element rootBuilder = XmlBuilder
            .root("classpath");

        addSourceDirIfExists(rootBuilder, module.getPath(), LoomPaths.SRC_MAIN);
        addSourceDirIfExists(rootBuilder, module.getPath(), LoomPaths.RES_MAIN);
        addSourceDirIfExists(rootBuilder, module.getPath(), LoomPaths.SRC_TEST);
        addSourceDirIfExists(rootBuilder, module.getPath(), LoomPaths.RES_TEST);

        rootBuilder.element("classpathentry")
            .attr("kind", "output")
            .attr("path", "bin");


        final ModuleBuildConfig moduleConfig = module.getConfig();
        rootBuilder.element("classpathentry")
            .attr("kind", "con")
            .attr("path",
                String.format("org.eclipse.jdt.launching.JRE_CONTAINER/"
                        + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s/",
                    buildProjectJdkName(moduleConfig.getBuildSettings().getJavaPlatformVersion())));

        for (final Module depModule : moduleGraph.get(module)) {
            rootBuilder.element("classpathentry")
                .attr("combineaccessrules", "false")
                .attr("kind", "src")
                .attr("path", "/" + depModule.getModuleName());
        }

        final Optional<ArtifactListProduct> testArtifacts =
            useProduct(module.getModuleName(), "testArtifacts", ArtifactListProduct.class);

        testArtifacts.map(ArtifactListProduct::getArtifacts).orElse(Collections.emptyList())
            .forEach(artifactProduct -> buildClasspathElement(rootBuilder, artifactProduct));

        return rootBuilder.getDocument();
    }

    private void addSourceDirIfExists(final XmlBuilder.Element root, final Path modulePath,
                                      final Path path) {
        if (Files.exists(modulePath.resolve(path))) {
            root.element("classpathentry")
                .attr("kind", "src")
                .attr("path", path.toString());
        }
    }

    private void buildClasspathElement(final XmlBuilder.Element rootBuilder,
                                       final ArtifactProduct artifactProduct) {

        final String jar = artifactProduct.getMainArtifact().toAbsolutePath().toString();
        final Path sourceArtifact = artifactProduct.getSourceArtifact();
        final Optional<String> sourceJar =
            Optional.ofNullable(sourceArtifact)
                .map(Path::toAbsolutePath)
                .map(Path::toString);

        final XmlBuilder.Element classpathentry = rootBuilder.element("classpathentry");

        sourceJar.ifPresent(path -> classpathentry.attr("sourcepath", path));
        classpathentry.attr("kind", "lib");
        classpathentry.attr("path", jar);

    }

}
