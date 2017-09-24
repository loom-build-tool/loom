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

package builders.loom.plugin.idea;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.w3c.dom.Document;

import builders.loom.api.AbstractTask;
import builders.loom.api.JavaVersion;
import builders.loom.api.Module;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.Product;
import builders.loom.util.xml.XmlBuilder;
import builders.loom.util.xml.XmlWriter;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class IdeaTask extends AbstractTask implements ModuleGraphAware {

    private Map<Module, Set<Module>> moduleGraph;

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> transitiveModuleGraph) {
        this.moduleGraph = transitiveModuleGraph;
    }

    @Override
    public TaskResult run(final boolean skip) throws Exception {
        final Path projectBaseDir = getRuntimeConfiguration().getProjectBaseDir();
        final Path ideaDirectory = Files.createDirectories(projectBaseDir.resolve(".idea"));

        final XmlWriter xmlWriter = new XmlWriter();
        final JavaVersion projectJavaVersion = determineModulesHighestJavaVersion();
        final List<IdeaModule> ideaModules = new ArrayList<>();

        // encodings.xml
        xmlWriter.write(createEncodingsFile(), ideaDirectory.resolve("encodings.xml"));

        // misc.xml
        xmlWriter.write(createMiscFile(projectJavaVersion), ideaDirectory.resolve("misc.xml"));

        // 1-n module (.iml) files
        for (final Module module : moduleGraph.keySet()) {
            final String moduleName = IdeaUtil.ideaModuleName(module.getPath());
            final Path imlFile = IdeaUtil.imlFileFromPath(module.getPath(), moduleName);
            xmlWriter.write(createModuleImlFile(imlFile, module, projectJavaVersion), imlFile);
            ideaModules.add(new IdeaModule(imlFile, moduleName));
        }

        if (getRuntimeConfiguration().isModuleBuild()) {
            // create separate umbrella .iml for multi-module projects
            final String moduleName = IdeaUtil.ideaModuleName(projectBaseDir);
            final Path rootImlFile = IdeaUtil.imlFileFromPath(projectBaseDir, moduleName);
            xmlWriter.write(createUmbrellaImlFile(), rootImlFile);
            ideaModules.add(new IdeaModule(rootImlFile, moduleName));
        }

        // modules.xml file containing referencing all modules
        xmlWriter.write(createModulesFile(ideaModules), ideaDirectory.resolve("modules.xml"));

        return TaskResult.ok(newProduct());
    }

    private JavaVersion determineModulesHighestJavaVersion() {
        return moduleGraph.keySet().stream()
            .map(m -> m.getConfig().getBuildSettings().getJavaPlatformVersion())
            .max(Comparator.comparingInt(JavaVersion::getNumericVersion))
            .orElseGet(JavaVersion::current);
    }

    private Document createEncodingsFile() {
        return XmlBuilder
            .root("project").attr("version", "4")
            .element("component").attr("name", "Encoding")
            .element("file")
                .attr("url", "PROJECT")
                .attr("charset", "UTF-8")
            .getDocument();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createMiscFile(final JavaVersion projectJavaVersion) {
        return XmlBuilder
            .root("project").attr("version", "4")
            .element("component")
                .attr("name", "ProjectRootManager")
                .attr("version", "2")
                .attr("languageLevel", buildLanguageLevel(projectJavaVersion))
                .attr("default", "false")
                .attr("project-jdk-name", buildJdkName(projectJavaVersion))
                .attr("project-jdk-type", "JavaSDK")
            .element("output").attr("url", "file://$PROJECT_DIR$/out")
            .getDocument();
    }

    private static String buildLanguageLevel(final JavaVersion javaVersion) {
        return "JDK_1_" + javaVersion.getNumericVersion();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static String buildJdkName(final JavaVersion javaVersion) {
        final int numericVersion = javaVersion.getNumericVersion();
        return (numericVersion < 9) ? "1." + numericVersion : String.valueOf(numericVersion);
    }

    private Document createModulesFile(final List<IdeaModule> ideaModules) {
        final XmlBuilder.Element element = XmlBuilder
            .root("project").attr("version", "4")
            .element("component").attr("name", "ProjectModuleManager")
            .element("modules");

        // integration test friendly predictable order
        ideaModules.stream()
            .sorted(Comparator.comparing(IdeaModule::getModuleName))
            .forEach(ideaModule -> {

                final String relativeImlFilename =
                    getRuntimeConfiguration().getProjectBaseDir()
                        .relativize(ideaModule.getImlFile()).toString();

                element
                    .element("module")
                    .attr("fileurl", "file://$PROJECT_DIR$/" + relativeImlFilename)
                    .attr("filepath", "$PROJECT_DIR$/" + relativeImlFilename);
            });

        return element.getDocument();
    }

    private Document createUmbrellaImlFile() {
        return XmlBuilder.root("module")
            .attr("type", "JAVA_MODULE")
            .attr("version", "4")
            .element("component")
                .attr("name", "NewModuleRootManager")
                .attr("inherit-compiler-output", "true")
            .element("exclude-output")
                .and()
            .element("content")
                .attr("url", "file://$MODULE_DIR$")
                .element("excludeFolder").attr("url", "file://$MODULE_DIR$/.loom").and()
                .element("excludeFolder").attr("url", "file://$MODULE_DIR$/build").and()
            .and()
            .element("orderEntry")
                .attr("type", "inheritedJdk")
                .and()
            .element("orderEntry")
                .attr("type", "sourceFolder")
                .attr("forTests", "false")
            .getDocument();
    }

    private Document createModuleImlFile(final Path imlFile, final Module module,
                                         final JavaVersion projectJavaVersion)
        throws InterruptedException {

        final XmlBuilder.Element moduleE = XmlBuilder.root("module")
            .attr("type", "JAVA_MODULE")
            .attr("version", "4");

        final XmlBuilder.Element component = moduleE.element("component")
                .attr("name", "NewModuleRootManager")
                .attr("inherit-compiler-output", "true");

        component.element("exclude-output");

        final String relativeModuleDir = buildRelativeModuleDir(
            imlFile.toAbsolutePath().getParent().relativize(module.getPath().toAbsolutePath()));

        final String mainSrcRoot = relativeModuleDir + "/src/main";
        final String testSrcRoot = relativeModuleDir + "/src/test";
        final XmlBuilder.Element content = component.element("content")
            .attr("url", relativeModuleDir);

        content.element("sourceFolder")
            .attr("url", mainSrcRoot + "/java")
            .attr("isTestSource", "false");

        content.element("sourceFolder")
            .attr("url", mainSrcRoot + "/resources")
            .attr("type", "java-resource");

        content.element("sourceFolder")
            .attr("url", testSrcRoot + "/java")
            .attr("isTestSource", "true");

        content.element("sourceFolder")
            .attr("url", testSrcRoot + "/resources")
            .attr("type", "java-test-resource");

        if (module.getPath().equals(getRuntimeConfiguration().getProjectBaseDir())) {
            content
                .element("excludeFolder").attr("url", "file://$MODULE_DIR$/.loom").and()
                .element("excludeFolder").attr("url", "file://$MODULE_DIR$/build");
        }

        final JavaVersion moduleJavaVersion =
            module.getConfig().getBuildSettings().getJavaPlatformVersion();
        final boolean inheritProjectJavaVersion = moduleJavaVersion.equals(projectJavaVersion);

        if (inheritProjectJavaVersion) {
            component.element("orderEntry")
                .attr("type", "inheritedJdk");
        } else {
            component.attr("LANGUAGE_LEVEL", buildLanguageLevel(moduleJavaVersion));

            component.element("orderEntry")
                .attr("type", "jdk")
                .attr("jdkName", buildJdkName(moduleJavaVersion))
                .attr("jdkType", "JavaSDK");
        }

        final OrderEntries orderEntries = new OrderEntries();

        // dependent modules
        for (final Module depModule : moduleGraph.get(module)) {
            component.element("orderEntry")
                .attr("type", "module")
                .attr("module-name", IdeaUtil.ideaModuleName(depModule.getPath()))
                .attr("scope", "COMPILE");

            // add compile artifacts of dependent module
            final List<String> compileArtifacts =
                useProduct(depModule.getModuleName(), "compileArtifacts", Product.class)
                    .map(p -> p.getProperties("artifacts"))
                    .orElse(Collections.emptyList());

            addOrderEntries(orderEntries, compileArtifacts, "COMPILE");
        }

        // add compile artifacts
        final List<String> compileArtifacts =
            useProduct(module.getModuleName(), "compileArtifacts", Product.class)
                .map(p -> p.getProperties("artifacts"))
                .orElse(Collections.emptyList());

        addOrderEntries(orderEntries, compileArtifacts, "COMPILE");

        // add test artifacts
        final List<String> testArtifacts =
            useProduct(module.getModuleName(), "testArtifacts", Product.class)
                .map(p -> p.getProperties("artifacts"))
                .orElse(Collections.emptyList());

        addOrderEntries(orderEntries, testArtifacts, "TEST");

        buildOrderEntries(component, orderEntries.getEntryList());

        component.element("orderEntry")
            .attr("type", "sourceFolder")
            .attr("forTests", "false");

        return moduleE.getDocument();
    }

    private void addOrderEntries(final OrderEntries orderEntries, final List<String> artifacts,
                                 final String scope) {
        for (final String artifact : artifacts) {
            // FIXME evil hack
            final String[] split = artifact.split("#");
            final Path mainArtifact = Paths.get(split[0]);
            final Path sourceArtifact = split[1].isEmpty() ? null : Paths.get(split[1]);
            orderEntries.append(mainArtifact, sourceArtifact, scope);
        }
    }

    private String buildRelativeModuleDir(final Path relativeModulePath) {
        final String relativeModuleDir = "file://$MODULE_DIR$/" + relativeModulePath;
        if (relativeModuleDir.endsWith("/")) {
            return relativeModuleDir.substring(0, relativeModuleDir.length() - 1);
        }
        return relativeModuleDir;
    }

    private void buildOrderEntries(final XmlBuilder.Element component,
                                   final Collection<OrderEntry> orderEntries) {

        // sort for predictable (testable) result
        final Stream<OrderEntry> sortedOrderEntries = orderEntries.stream()
            .sorted(Comparator.comparing(OrderEntry::getScope)
                .thenComparing(o -> o.getMainArtifact().getFileName().toString()));

        sortedOrderEntries.forEachOrdered(orderEntry -> {
            final String mainJar = orderEntry.getMainArtifact().toAbsolutePath().toString();
            final Path sourceArtifact = orderEntry.getSourceArtifact();
            final String sourceJar = sourceArtifact != null
                ? sourceArtifact.toAbsolutePath().toString() : null;

            buildOrderEntry(component.element("orderEntry"),
                orderEntry.getScope(), replaceUserHome(mainJar), replaceUserHome(sourceJar));
        });
    }

    private String replaceUserHome(final String path) {
        if (path == null) {
            return null;
        }
        return path.replace(System.getProperty("user.home"), "$USER_HOME$");
    }

    private void buildOrderEntry(final XmlBuilder.Element orderEntry,
                                 final String scope, final String jar, final String sourceJar) {

        orderEntry.attr("type", "module-library");
        if (scope != null) {
            orderEntry.attr("scope", scope);
        }

        final XmlBuilder.Element library = orderEntry.element("library");
        appendJarElement(library, "CLASSES", jar);
        appendJarElement(library, "SOURCES", sourceJar);
        appendJarElement(library, "JAVADOC", null);
    }

    private void appendJarElement(final XmlBuilder.Element library, final String name,
                                  final String jar) {
        final XmlBuilder.Element libHolder = library.element(name);
        if (jar != null) {
            libHolder.element("root")
                .attr("url", String.format("jar://%s!/", jar));
        }
    }

    private static Product newProduct() {
        return new GenericProduct(Collections.emptyMap(), UUID.randomUUID().toString(),
            "Idea project files");
    }

}
