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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.JavaVersion;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;
import builders.loom.util.xml.XmlBuilder;
import builders.loom.util.xml.XmlWriter;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class IdeaTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final XmlWriter xmlWriter = new XmlWriter();
    private GlobalProductRepository globalProductRepository;

    public IdeaTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public void setGlobalProductRepository(final GlobalProductRepository globalProductRepository) {
        this.globalProductRepository = globalProductRepository;
    }

    @Override
    public TaskResult run() throws Exception {
        final Path currentWorkDirName = LoomPaths.PROJECT_DIR.getFileName();

        if (currentWorkDirName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        final Path ideaDirectory = Files.createDirectories(LoomPaths.PROJECT_DIR.resolve(".idea"));

        xmlWriter.write(createEncodingsFile(), ideaDirectory.resolve("encodings.xml"));
        xmlWriter.write(createMiscFile(), ideaDirectory.resolve("misc.xml"));
        xmlWriter.write(createModulesFile(buildIdeaModules(currentWorkDirName, ideaDirectory)),
            ideaDirectory.resolve("modules.xml"));

        return completeOk(new DummyProduct("Idea project files"));
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
    private Document createMiscFile() {
        final JavaVersion javaVersion = buildConfig.getBuildSettings().getJavaPlatformVersion();

        return XmlBuilder
            .root("project").attr("version", "4")
            .element("component")
            .attr("name", "ProjectRootManager")
            .attr("version", "2")
            .attr("languageLevel", buildLanguageLevel(javaVersion))
            .attr("default", "false") // TODO gradle == false || intellij setup == true !?!?
            .attr("project-jdk-name", buildProjectJdkName(javaVersion))
            .attr("project-jdk-type", "JavaSDK")
            .element("output").attr("url", "file://$PROJECT_DIR$/out")
            .getDocument();
    }

    private static String buildLanguageLevel(final JavaVersion javaVersion) {
        return "JDK_1_" + javaVersion.getNumericVersion();
    }

    private static String buildProjectJdkName(final JavaVersion javaVersion) {
        final int numericVersion = javaVersion.getNumericVersion();
        return (numericVersion < 9) ? "1." + numericVersion : String.valueOf(numericVersion);
    }

    private List<IdeaModule> buildIdeaModules(final Path currentWorkDirName, final Path ideaDirectory) throws InterruptedException, IOException {
        final List<IdeaModule> ideaModules = new ArrayList<>();
        for (final Module module : globalProductRepository.getAllModules()) {
            if (module.getModuleName().equals("unnamed")) {
                final Path imlFile = LoomPaths.PROJECT_DIR.resolve(currentWorkDirName + ".iml");
                final String imlFileName = LoomPaths.PROJECT_DIR.relativize(imlFile).toString();
                xmlWriter.write(createImlFile(imlFile, module, ModuleGroup.BASE), imlFile);
                ideaModules.add(new IdeaModule(module.getModuleName(), null, imlFileName));
            } else {
                for (final ModuleGroup group : ModuleGroup.values()) {

                    final Path ideaModulesDir = Files.createDirectories(ideaDirectory.resolve(Paths.get("modules", module.getModuleName())));

                    final Path imlFile = ideaModulesDir.resolve(buildImlFilename(module, group));
                    final String imlFileName = LoomPaths.PROJECT_DIR.relativize(imlFile).toString();
                    xmlWriter.write(createImlFile(imlFile, module, group), imlFile);
                    ideaModules.add(new IdeaModule(module.getModuleName(), group, imlFileName));
                }
            }

        }
        return ideaModules;
    }

    private String buildImlFilename(final Module module, final ModuleGroup group) {
        final StringBuilder filenameSb = new StringBuilder(module.getModuleName());
        if (group != ModuleGroup.BASE) {
            filenameSb.append('_');
            filenameSb.append(group.name().toLowerCase());
        }
        filenameSb.append(".iml");
        return filenameSb.toString();
    }

    private Document createModulesFile(final List<IdeaModule> ideaModules) {
        final XmlBuilder.Element element = XmlBuilder
            .root("project").attr("version", "4")
            .element("component").attr("name", "ProjectModuleManager")
            .element("modules");

        for (final IdeaModule ideaModule : ideaModules) {
            final XmlBuilder.Element module = element
                .element("module")
                .attr("fileurl", "file://$PROJECT_DIR$/" + ideaModule.getFilename())
                .attr("filepath", "$PROJECT_DIR$/" + ideaModule.getFilename());

            if (ideaModule.getGroup() != null) {
                module.attr("group", ideaModule.getModuleName());
            }
        }

        return element.getDocument();
    }

    private Document createImlFile(final Path imlFile, final Module module, final ModuleGroup group) throws InterruptedException {
        final XmlBuilder.Element moduleE = XmlBuilder.root("module")
            .attr("type", "JAVA_MODULE")
            .attr("version", "4");

        final XmlBuilder.Element component = moduleE
            .element("component").attr("name", "NewModuleRootManager");

        component.element("exclude-output");

        final String relativeModuleDir =
            buildRelativeModuleDir(imlFile.getParent().relativize(module.getPath()));

        if (group == ModuleGroup.MAIN) {
            buildMainComponent(module, component, relativeModuleDir);
        } else if (group == ModuleGroup.TEST) {
            buildTestComponent(module, component, relativeModuleDir);
        } else {
            buildBaseComponent(component, relativeModuleDir);
        }

        component.element("orderEntry")
            .attr("type", "inheritedJdk");

        component.element("orderEntry")
            .attr("type", "sourceFolder")
            .attr("forTests", "false");

        return moduleE.getDocument();
    }

    private String buildRelativeModuleDir(final Path relativeModulePath) {
        final String relativeModuleDir = "file://$MODULE_DIR$/" + relativeModulePath;
        if (relativeModuleDir.endsWith("/")) {
            return relativeModuleDir.substring(0, relativeModuleDir.length() - 1);
        }
        return relativeModuleDir;
    }

    private void buildMainComponent(final Module module, final XmlBuilder.Element component, final String relativeModuleDir) throws InterruptedException {
        component.attr("LANGUAGE_LEVEL", buildLanguageLevel(module.getConfig().getBuildSettings().getJavaPlatformVersion()));

        component.element("output")
            .attr("url", relativeModuleDir + "/out/production/classes");

        final String srcRoot = relativeModuleDir + "/src/main";
        final XmlBuilder.Element content = component.element("content")
            .attr("url", srcRoot);

        content.element("sourceFolder")
            .attr("url", srcRoot + "/java")
            .attr("isTestSource", "false");

        content.element("sourceFolder")
            .attr("url", srcRoot + "/resources")
            .attr("type", "java-resource");

        // dependent modules
        for (final String depModule : module.getConfig().getModuleDependencies()) {
            component.element("orderEntry")
                .attr("type", "module")
                .attr("module-name", depModule + "_main")
                .attr("scope", "PROVIDED");
        }

        // add compile artifacts
        globalProductRepository
            .useProduct(module.getModuleName(), "compileArtifacts", ArtifactListProduct.class)
            .map(ArtifactListProduct::getArtifacts)
            .ifPresent(artifacts -> buildOrderEntries(component, artifacts, "COMPILE"));
    }

    private void buildTestComponent(final Module module, final XmlBuilder.Element component, final String relativeModuleDir) throws InterruptedException {
        component.attr("LANGUAGE_LEVEL", buildLanguageLevel(module.getConfig().getBuildSettings().getJavaPlatformVersion()));

        component.element("output-test")
            .attr("url", relativeModuleDir + "/out/test/classes");

        final String srcRoot = relativeModuleDir + "/src/test";
        final XmlBuilder.Element content = component.element("content")
            .attr("url", srcRoot);

        content.element("sourceFolder")
            .attr("url", srcRoot + "/java")
            .attr("isTestSource", "true");

        content.element("sourceFolder")
            .attr("url", srcRoot + "/resources")
            .attr("type", "java-test-resource");

        // main module
        component.element("orderEntry")
            .attr("type", "module")
            .attr("module-name", module.getModuleName() + "_main");

        // TODO in module: <component name="TestModuleProperties" production-module="plugin-springboot_main" />

        // add test artifacts
        globalProductRepository
            .useProduct(module.getModuleName(), "testArtifacts", ArtifactListProduct.class)
            .map(ArtifactListProduct::getArtifacts)
            .ifPresent(artifacts -> buildOrderEntries(component, artifacts, "TEST"));
    }

    private void buildBaseComponent(final XmlBuilder.Element component, final String relativeModuleDir) {
        component.attr("inherit-compiler-output", "true");

        final XmlBuilder.Element content = component.element("content")
            .attr("url", relativeModuleDir);

        content.element("excludeFolder").attr("url", relativeModuleDir + "/out");

        // TODO nur im root
        content.element("excludeFolder").attr("url", relativeModuleDir + "/.loom");
        content.element("excludeFolder").attr("url", relativeModuleDir + "/loombuild");
    }

    private void buildOrderEntries(final XmlBuilder.Element component,
                                   final List<ArtifactProduct> mainArtifacts, final String scope) {
        for (final ArtifactProduct artifact : mainArtifacts) {
            final String mainJar = artifact.getMainArtifact().toAbsolutePath().toString();
            final Path sourceArtifact = artifact.getSourceArtifact();
            final String sourceJar = sourceArtifact != null
                ? sourceArtifact.toAbsolutePath().toString() : null;

            buildOrderEntry(component.element("orderEntry"), scope, mainJar, sourceJar);
        }
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

    private void appendJarElement(final XmlBuilder.Element library, final String name, final String jar) {
        final XmlBuilder.Element libHolder = library.element(name);
        if (jar != null) {
            libHolder.element("root")
                .attr("url", String.format("jar://%s!/", jar));
        }
    }

}
