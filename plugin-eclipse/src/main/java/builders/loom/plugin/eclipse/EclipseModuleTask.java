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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
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
import org.xml.sax.SAXException;

import builders.loom.api.AbstractTask;
import builders.loom.api.JavaVersion;
import builders.loom.api.Module;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;
import builders.loom.util.xml.XmlBuilder;

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

    private Set<Module> allModules() {
        return moduleGraph.keySet();
    }

    private static InputStream readResource(final String resourceName) {
        return new BufferedInputStream(EclipseModuleTask.class.getResourceAsStream(resourceName));
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

    private void createModuleProject(final Module module)
        throws IOException, SAXException, TransformerException, InterruptedException {
        writeDocumentToFile(module.getPath().resolve(".project"), createProjectFile(module));
        writeDocumentToFile(module.getPath().resolve(".classpath"), createClasspathFile(module));
        final Path settingsPath = Files.createDirectories(module.getPath().resolve(".settings"));
        writePropertiesToFile(settingsPath.resolve("org.eclipse.jdt.core.prefs"), createJdtPrefs(module));
    }

    private Properties createJdtPrefs(Module module) {

        Properties prefs = new Properties();
        String javaLangLevel = buildProjectJdkName(module.getConfig().getBuildSettings().getJavaPlatformVersion());

        prefs.setProperty("eclipse.preferences.version", "1");
        prefs.setProperty("org.eclipse.jdt.core.compiler.source", javaLangLevel);
        prefs.setProperty("org.eclipse.jdt.core.compiler.compliance", javaLangLevel);

        return prefs;
    }

    private static String buildProjectJdkName(final JavaVersion javaVersion) {
        final int numericVersion = javaVersion.getNumericVersion();
        return (numericVersion < 9) ? "1." + numericVersion : String.valueOf(numericVersion);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createProjectFile(final Module module) throws IOException, SAXException {
        try (final InputStream resourceAsStream = readResource("/project-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Node component = doc.getDocumentElement()
                .getElementsByTagName("name").item(0);

            component.setTextContent(module.getModuleName());

            return doc;
        }
    }

    private void writeDocumentToFile(final Path file, final Document doc)
        throws IOException, TransformerException {

        try (final OutputStream outputStream = newOut(file)) {
            doc.setXmlStandalone(true);

            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        }
    }

    private void writePropertiesToFile(Path file, Properties properties) throws IOException {

        try (final OutputStream outputStream = newOut(file)) {
            properties.store(outputStream, "Loom Eclipse Plugin");
        }

    }

    private OutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createClasspathFile(Module module)
        throws IOException, InterruptedException, SAXException {

        final XmlBuilder.Element rootBuilder = XmlBuilder
            .root("classpath");

        addSourceDirIfExists(rootBuilder, module.getPath(), Paths.get("src", "main", "java"));
        addSourceDirIfExists(rootBuilder, module.getPath(), Paths.get("src", "main", "resources"));
        addSourceDirIfExists(rootBuilder, module.getPath(), Paths.get("src", "test", "java"));
        addSourceDirIfExists(rootBuilder, module.getPath(), Paths.get("src", "test", "resources"));

        final Document doc = rootBuilder.element("classpathentry")
            .attr("kind", "output")
            .attr("path", "bin")
            .getDocument();

        final Element root = doc.getDocumentElement();

        final Element jdkEntry = doc.createElement("classpathentry");
        jdkEntry.setAttribute("kind", "con");
        jdkEntry.setAttribute("path",
            String.format("org.eclipse.jdt.launching.JRE_CONTAINER/"
                + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s/",
                buildProjectJdkName(module.getConfig().getBuildSettings().getJavaPlatformVersion())));
        root.appendChild(jdkEntry);

        for (final Module depModule : moduleGraph.get(module)) {
            final Element projectRef = doc.createElement("classpathentry");
            projectRef.setAttribute("combineaccessrules", "false");
            projectRef.setAttribute("kind", "src");
            projectRef.setAttribute("path", "/" + depModule.getModuleName());
            root.appendChild(projectRef);
        }

        final Optional<ArtifactListProduct> testArtifacts =
            useProduct(module.getModuleName(),"testArtifacts", ArtifactListProduct.class);

            testArtifacts.map(ArtifactListProduct::getArtifacts).orElse(Collections.emptyList()).stream()
            .forEach(artifactProduct -> {
                root.appendChild(buildClasspathElement(doc, artifactProduct));
            });

        return doc;
    }

    private void addSourceDirIfExists(XmlBuilder.Element root, final Path modulePath, Path path) {
        if (Files.exists(modulePath.resolve(path))) {
            root.element("classpathentry")
                .attr("kind", "src")
                .attr("path", path.toString());
        }
    }

    private Element buildClasspathElement(final Document doc,
                                          ArtifactProduct artifactProduct) {

        final String jar = artifactProduct.getMainArtifact().toAbsolutePath().toString();
        final Path sourceArtifact = artifactProduct.getSourceArtifact();
        final Optional<String> sourceJar =
            Optional.ofNullable(sourceArtifact)
                .map(Path::toAbsolutePath)
                .map(Path::toString);

        final Element classpathentry = doc.createElement("classpathentry");
        sourceJar.ifPresent(path -> classpathentry.setAttribute("sourcepath", path));
        classpathentry.setAttribute("kind", "lib");
        classpathentry.setAttribute("path", jar);

        return classpathentry;
    }

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> moduleGraph) {
        this.moduleGraph = moduleGraph;
    }
}
