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

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import builders.loom.api.AbstractTask;
import builders.loom.api.JavaVersion;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.util.Preconditions;
import builders.loom.util.ProductChecksumUtil;
import builders.loom.util.PropertiesMerger;
import builders.loom.util.xml.XmlBuilder;
import builders.loom.util.xml.XmlParser;
import builders.loom.util.xml.XmlUtil;
import builders.loom.util.xml.XmlWriter;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class EclipseTask extends AbstractTask implements ModuleGraphAware {

    // clean-create all eclipse files
    private Map<Module, Set<Module>> moduleGraph;

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> transitiveModuleGraph) {
        this.moduleGraph = transitiveModuleGraph;
    }

    @Override
    public TaskResult run() throws Exception {
        for (final Module module : allModules()) {
            createModuleProject(module);
        }

        return TaskResult.done(newProduct());
    }

    private Set<Module> allModules() {
        return moduleGraph.keySet();
    }

    private void createModuleProject(final Module module)
        throws IOException, InterruptedException {

        final XmlWriter xmlWriter = new XmlWriter();

        final Path projectXml = module.getPath().resolve(".project");

        // pickup and merge existing .project file or create a new one
        if (Files.notExists(projectXml)) {
            xmlWriter.write(createProjectFile(module), projectXml);
        } else {
            final XmlParser xmlParser = XmlParser.createXmlParser();
            final Document projectXmlDoc = xmlParser.parse(projectXml);
            final boolean changedBuildSpec = mergeProjectBuildSpec(projectXmlDoc);
            final boolean changedNature = mergeProjectNature(projectXmlDoc);
            if (changedBuildSpec || changedNature) {
                xmlWriter.write(projectXmlDoc, projectXml);
            }
        }

        // always create a new .classpath file
        xmlWriter.write(createClasspathFile(module), module.getPath().resolve(".classpath"));

        // pickup and merge existing .prefs file or create a new one
        final Path settingsFile = Files.createDirectories(module.getPath().resolve(".settings"))
            .resolve("org.eclipse.jdt.core.prefs");
        if (Files.notExists(settingsFile)) {
            writePropertiesToFile(settingsFile, createJdtPrefs(module));
        } else {
            final Properties props = new Properties();
            props.load(new FileReader(settingsFile.toFile()));
            if (mergeJdtPrefs(props, module)) {
                writePropertiesToFile(settingsFile, props);
            }
        }
    }

    private Properties createJdtPrefs(final Module module) {

        final String javaLangLevel =
            buildProjectJdkName(module.getConfig().getBuildSettings().getJavaPlatformVersion());

        final Properties prefs = new Properties();

        prefs.setProperty("eclipse.preferences.version", "1");
        prefs.setProperty("org.eclipse.jdt.core.compiler.source", javaLangLevel);
        prefs.setProperty("org.eclipse.jdt.core.compiler.compliance", javaLangLevel);
        prefs.setProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform", javaLangLevel);

        addJdtPrefDefaults(prefs);

        return prefs;
    }

    private boolean mergeJdtPrefs(final Properties prefs, final Module module) {

        Preconditions.checkState(prefs.getProperty("eclipse.preferences.version").equals("1"));

        final String javaLangLevel =
            buildProjectJdkName(module.getConfig().getBuildSettings().getJavaPlatformVersion());

        final PropertiesMerger merger = new PropertiesMerger(prefs);

        merger.set("org.eclipse.jdt.core.compiler.source", javaLangLevel);
        merger.set("org.eclipse.jdt.core.compiler.compliance", javaLangLevel);
        merger.set("org.eclipse.jdt.core.compiler.codegen.targetPlatform", javaLangLevel);

        final boolean changedByDefaults = addJdtPrefDefaults(prefs);

        return merger.isChanged() || changedByDefaults;
    }

    private boolean addJdtPrefDefaults(final Properties prefs) {

        final PropertiesMerger merger = new PropertiesMerger(prefs);

        merger.setIfAbsent("org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode", "enabled");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.codegen.unusedLocal", "preserve");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.problem.assertIdentifier", "error");
        merger.setIfAbsent("org.eclipse.jdt.core.compiler.problem.enumIdentifier", "error");

        return merger.isChanged();
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

        final Element buildSpec =
            XmlUtil.getOnlyElement(projectXml.getElementsByTagName("buildSpec"));

        for (final Element item
            : XmlUtil.iterableElements(projectXml.getElementsByTagName("buildCommand"))) {

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

        final Element naturesNode =
            XmlUtil.getOnlyElement(projectXml.getElementsByTagName("natures"));

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

    private void writePropertiesToFile(final Path file, final Properties properties)
        throws IOException {

        try (final OutputStream outputStream = XmlWriter.newOut(file)) {
            properties.store(outputStream, "Loom Eclipse Plugin");
        }

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
                .attr("path", "/" + depModule.getModuleName())
                .element("attributes")
                    .element("attribute")
                        .attr("name", "module")
                        .attr("value", "true");
        }

        final List<String> testArtifacts =
            useProduct(module.getModuleName(), "testArtifacts", Product.class)
            .map(p -> p.getProperties("artifacts"))
            .orElse(Collections.emptyList());

        for (final String testArtifact : testArtifacts) {
            // FIXME evil hack
            final String[] split = testArtifact.split("#");
            final Path mainArtifact = Paths.get(split[0]);
            final Path sourceArtifact = split[1].isEmpty() ? null : Paths.get(split[1]);
            buildClasspathElement(rootBuilder, mainArtifact, sourceArtifact);
        }

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
                                       final Path mainArtifact, final Path sourceArtifact) {

        final String jar = mainArtifact.toAbsolutePath().toString();
        final Optional<String> sourceJar =
            Optional.ofNullable(sourceArtifact)
                .map(Path::toAbsolutePath)
                .map(Path::toString);

        final XmlBuilder.Element classpathentry = rootBuilder.element("classpathentry");

        sourceJar.ifPresent(path -> classpathentry.attr("sourcepath", path));
        classpathentry.attr("kind", "lib");
        classpathentry.attr("path", jar);

    }

    private static Product newProduct() {
        return new GenericProduct(Collections.emptyMap(), ProductChecksumUtil.random(),
            new OutputInfo("Eclipse project files"));
    }

}
