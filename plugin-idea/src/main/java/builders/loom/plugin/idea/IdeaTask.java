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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import builders.loom.api.BuildConfig;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class IdeaTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final DocumentBuilder docBuilder;
    private final Transformer transformer;

    public IdeaTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;

        try {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            docBuilder = dbFactory.newDocumentBuilder();

            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        } catch (final ParserConfigurationException | TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream readResource(final String resourceName) {
        return new BufferedInputStream(IdeaTask.class.getResourceAsStream(resourceName));
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
    private Document createMiscFile() throws IOException, SAXException {
        try (final InputStream resourceAsStream = readResource("/misc-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Element component = getOnlyElementByTagName(doc, "component");

            final String languageLevel;
            final String projectJdkName;
            final int javaPlatformVersion = buildConfig.getBuildSettings().getJavaPlatformVersion()
                .getNumericVersion();

            languageLevel = "JDK_1_" + javaPlatformVersion;
            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = String.valueOf(javaPlatformVersion);
            }

            component.setAttribute("languageLevel", languageLevel);
            component.setAttribute("project-jdk-name", projectJdkName);

            return doc;
        }
    }

    private Element getOnlyElementByTagName(final Document doc, final String tagName) {
        final NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList.getLength() != 1) {
            throw new IllegalStateException("Expected NodeList length: 1, but was "
                + nodeList.getLength());
        }
        final Node item = nodeList.item(0);

        if (!(item instanceof Element)) {
            throw new IllegalStateException("Item is not of type Element: " + item);
        }

        return (Element) item;
    }

    private void writeDocumentToFile(final Path file, final Document doc)
        throws IOException, TransformerException {

        try (final OutputStream outputStream = newOut(file)) {
            doc.setXmlStandalone(true);

            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        }
    }

    private OutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    private Document createModulesFile(final String imlFilename)
        throws IOException, SAXException {
        try (final InputStream resourceAsStream = readResource("/modules-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Element modules = getOnlyElementByTagName(doc, "modules");

            final Element module = doc.createElement("module");
            module.setAttribute("fileurl", "file://$PROJECT_DIR$/" + imlFilename);
            module.setAttribute("filepath", "$PROJECT_DIR$/" + imlFilename);
            modules.appendChild(module);

            return doc;
        }
    }

    private Document createImlFile() throws IOException, InterruptedException, SAXException {
        try (final InputStream resourceAsStream = readResource("/iml-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Element component = getOnlyElementByTagName(doc, "component");

            // add compile artifacts
            final Optional<ArtifactListProduct> compileArtifacts =
                useProduct("compileArtifacts", ArtifactListProduct.class);
            compileArtifacts
                .map(ArtifactListProduct::getArtifacts)
                .ifPresent(artifacts -> buildOrderEntries(doc, component, artifacts, "COMPILE"));

            // add test artifacts
            final Optional<ArtifactListProduct> testArtifacts =
                useProduct("testArtifacts", ArtifactListProduct.class);
            testArtifacts
                .map(ArtifactListProduct::getArtifacts)
                .ifPresent(artifacts -> buildOrderEntries(doc, component, artifacts, "TEST"));

            return doc;
        }
    }

    private void buildOrderEntries(final Document doc, final Node component,
                                   final List<ArtifactProduct> mainArtifacts, final String scope) {
        for (final ArtifactProduct artifact : mainArtifacts) {
            final String mainJar = artifact.getMainArtifact().toAbsolutePath().toString();
            final Path sourceArtifact = artifact.getSourceArtifact();
            final String sourceJar = sourceArtifact != null
                ? sourceArtifact.toAbsolutePath().toString() : null;

            component.appendChild(buildOrderEntry(doc, scope, mainJar, sourceJar));
        }
    }

    private Element buildOrderEntry(final Document doc, final String scope,
                                    final String jar, final String sourceJar) {
        final Element orderEntry = doc.createElement("orderEntry");
        orderEntry.setAttribute("type", "module-library");
        if (scope != null) {
            orderEntry.setAttribute("scope", scope);
        }

        final Element library = doc.createElement("library");
        library.appendChild(buildJarElement(doc, "CLASSES", jar));
        library.appendChild(buildJarElement(doc, "SOURCES", sourceJar));
        library.appendChild(buildJarElement(doc, "JAVADOC", null));
        orderEntry.appendChild(library);

        return orderEntry;
    }

    private Element buildJarElement(final Document doc, final String name, final String jar) {
        final Element classes = doc.createElement(name);
        if (jar != null) {
            final Element root = doc.createElement("root");
            root.setAttribute("url", String.format("jar://%s!/", jar));
            classes.appendChild(root);
        }
        return classes;
    }

}
