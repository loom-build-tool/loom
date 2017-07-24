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
import org.xml.sax.SAXException;

import builders.loom.api.AbstractTask;
import builders.loom.api.JavaVersion;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.DummyProduct;

@SuppressWarnings("checkstyle:classfanoutcomplexity")
public class EclipseModuleTask extends AbstractTask {

    private final DocumentBuilder docBuilder;
    private final Transformer transformer;

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

        final String projectName = currentWorkDirName.toString();
        writeDocumentToFile(currentDir.resolve(".project"), createProjectFile(projectName));
        writeDocumentToFile(currentDir.resolve(".classpath"), createClasspathFile());

        return completeOk(new DummyProduct("Eclipse project files"));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createProjectFile(final String name) throws IOException, SAXException {
        try (final InputStream resourceAsStream = readResource("/project-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Node component = doc.getDocumentElement()
                .getElementsByTagName("name").item(0);

            component.setTextContent(name);

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

    private OutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createClasspathFile()
        throws IOException, InterruptedException, SAXException {

        try (final InputStream resourceAsStream = readResource("/classpath-template.xml")) {
            final Document doc = docBuilder.parse(resourceAsStream);
            final Element root = doc.getDocumentElement();

            final String projectJdkName;
            final int javaPlatformVersion = JavaVersion.current() // FIXME
                .getNumericVersion();

            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = String.valueOf(javaPlatformVersion);
            }

            final Element jdkEntry = doc.createElement("classpathentry");
            jdkEntry.setAttribute("kind", "con");
            jdkEntry.setAttribute("path",
                String.format("org.eclipse.jdt.launching.JRE_CONTAINER/"
                    + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s/",
                    projectJdkName));
            root.appendChild(jdkEntry);

            final Optional<ArtifactListProduct> testArtifacts =
                useProduct("testArtifacts", ArtifactListProduct.class);

            testArtifacts.ifPresent(artifactListProduct -> {
                for (final ArtifactProduct path : artifactListProduct.getArtifacts()) {
                    final String jar = path.getMainArtifact().toAbsolutePath().toString();
                    final Path sourceArtifact = path.getSourceArtifact();
                    final String sourceJar = sourceArtifact != null
                        ? sourceArtifact.toAbsolutePath().toString() : null;

                    root.appendChild(buildClasspathElement(doc, jar, sourceJar));
                }
            });

            return doc;
        }
    }

    private Element buildClasspathElement(final Document doc,
                                          final String jar, final String sourceJar) {

        final Element classpathentry = doc.createElement("classpathentry");
        if (sourceJar != null) {
            classpathentry.setAttribute("sourcepath", sourceJar);
        }
        classpathentry.setAttribute("kind", "lib");
        classpathentry.setAttribute("path", jar);

        return classpathentry;
    }

}
