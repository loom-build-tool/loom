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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class IdeaTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final Builder parser = new Builder();

    public IdeaTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    private static InputStream readResource(final String resourceName) {
        return IdeaTask.class.getResourceAsStream(resourceName);
    }

    @Override
    public TaskResult run() throws Exception {
        final Path currentDir = Paths.get("");

        final Path currentWorkDirName = currentDir.toAbsolutePath().getFileName();

        if (currentWorkDirName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        final Path ideaDirectory = Files.createDirectories(currentDir.resolve(".idea"));

        createEncodingsFile(ideaDirectory.resolve("encodings.xml"));
        writeDocumentToFile(ideaDirectory.resolve("misc.xml"), createMiscFile());

        final String imlFilename = currentWorkDirName.toString() + ".iml";
        writeDocumentToFile(currentDir.resolve(imlFilename), createImlFile());
        writeDocumentToFile(ideaDirectory.resolve("modules.xml"), createModulesFile(imlFilename));

        return completeOk(new DummyProduct("Idea project files"));
    }

    private void createEncodingsFile(final Path encodingsFile) throws IOException {
        try (final InputStream resourceAsStream = readResource("/encodings.xml")) {
            Files.copy(resourceAsStream, encodingsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createMiscFile() throws IOException, ParsingException {
        try (final InputStream resourceAsStream = readResource("/misc-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element component = doc.getRootElement().getFirstChildElement("component");

            final String languageLevel;
            final String projectJdkName;
            final int javaPlatformVersion = buildConfig.getBuildSettings().getJavaPlatformVersion()
                .getNumericVersion();

            languageLevel = "JDK_1_" + javaPlatformVersion;
            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = "" + javaPlatformVersion;
            }

            component.addAttribute(new Attribute("languageLevel", languageLevel));
            component.addAttribute(new Attribute("project-jdk-name", projectJdkName));

            return doc;
        }
    }

    private void writeDocumentToFile(final Path file, final Document doc) throws IOException {
        try (final OutputStream outputStream = Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            final Serializer serializer = new Serializer(outputStream, "UTF-8");
            serializer.setIndent(2);
            serializer.write(doc);
        }
    }

    private Document createModulesFile(final String imlFilename)
        throws IOException, ParsingException {
        try (final InputStream resourceAsStream = readResource("/modules-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element modules = doc.getRootElement().getFirstChildElement("component")
                .getFirstChildElement("modules");

            final Element module = new Element("module");
            module.addAttribute(new Attribute("fileurl", "file://$PROJECT_DIR$/" + imlFilename));
            module.addAttribute(new Attribute("filepath", "$PROJECT_DIR$/" + imlFilename));
            modules.appendChild(module);

            return doc;
        }
    }

    private Document createImlFile() throws IOException, ParsingException, InterruptedException {
        try (final InputStream resourceAsStream = readResource("/iml-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element component = doc.getRootElement().getFirstChildElement("component");

            // add compile artifacts
            final Optional<ArtifactListProduct> compileArtifacts =
                useProduct("compileArtifacts", ArtifactListProduct.class);
            compileArtifacts
                .map(ArtifactListProduct::getArtifacts)
                .ifPresent(artifacts -> buildOrderEntries(component, artifacts, "COMPILE"));

            // add test artifacts
            final Optional<ArtifactListProduct> testArtifacts =
                useProduct("testArtifacts", ArtifactListProduct.class);
            testArtifacts
                .map(ArtifactListProduct::getArtifacts)
                .ifPresent(artifacts -> buildOrderEntries(component, artifacts, "TEST"));

            return doc;
        }
    }

    private void buildOrderEntries(final Element component,
                                   final List<ArtifactProduct> mainArtifacts, final String scope) {
        for (final ArtifactProduct artifact : mainArtifacts) {
            final String mainJar = artifact.getMainArtifact().toAbsolutePath().toString();
            final Path sourceArtifact = artifact.getSourceArtifact();
            final String sourceJar = sourceArtifact != null
                ? sourceArtifact.toAbsolutePath().toString() : null;

            component.appendChild(buildOrderEntry(scope, mainJar, sourceJar));
        }
    }

    private Element buildOrderEntry(final String scope, final String jar, final String sourceJar) {
        final Element orderEntry = new Element("orderEntry");
        orderEntry.addAttribute(new Attribute("type", "module-library"));
        if (scope != null) {
            orderEntry.addAttribute(new Attribute("scope", scope));
        }

        final Element library = new Element("library");
        library.appendChild(buildJarElement("CLASSES", jar));
        library.appendChild(buildJarElement("SOURCES", sourceJar));
        library.appendChild(buildJarElement("JAVADOC", null));
        orderEntry.appendChild(library);

        return orderEntry;
    }

    private Element buildJarElement(final String name, final String jar) {
        final Element classes = new Element(name);
        if (jar != null) {
            final Element root = new Element("root");
            root.addAttribute(new Attribute("url", String.format("jar://%s!/", jar)));
            classes.appendChild(root);
        }
        return classes;
    }

}
