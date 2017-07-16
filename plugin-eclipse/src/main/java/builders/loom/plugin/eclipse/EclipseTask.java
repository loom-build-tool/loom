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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

public class EclipseTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final Builder parser = new Builder();

    public EclipseTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    private static InputStream readResource(final String resourceName) {
        return EclipseTask.class.getResourceAsStream(resourceName);
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
    private Document createProjectFile(final String name) throws IOException, ParsingException {
        try (final InputStream resourceAsStream = readResource("/project-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element component = doc.getRootElement()
                .getFirstChildElement("name");

            component.appendChild(name);

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

    @SuppressWarnings("checkstyle:magicnumber")
    private Document createClasspathFile()
        throws IOException, ParsingException, InterruptedException {

        try (final InputStream resourceAsStream = readResource("/classpath-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element root = doc.getRootElement();

            final String projectJdkName;
            final int javaPlatformVersion = buildConfig.getBuildSettings().getJavaPlatformVersion()
                .getNumericVersion();

            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = String.valueOf(javaPlatformVersion);
            }

            final Element jdkEntry = new Element("classpathentry");
            jdkEntry.addAttribute(new Attribute("kind", "con"));
            jdkEntry.addAttribute(new Attribute("path",
                String.format("org.eclipse.jdt.launching.JRE_CONTAINER/"
                    + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s/",
                    projectJdkName)));
            root.appendChild(jdkEntry);

            final Optional<ArtifactListProduct> testArtifacts =
                useProduct("testArtifacts", ArtifactListProduct.class);

            testArtifacts.ifPresent(artifactListProduct -> {
                for (final ArtifactProduct path : artifactListProduct.getArtifacts()) {
                    final String jar = path.getMainArtifact().toAbsolutePath().toString();
                    final Path sourceArtifact = path.getSourceArtifact();
                    final String sourceJar = sourceArtifact != null
                        ? sourceArtifact.toAbsolutePath().toString() : null;

                    root.appendChild(buildClasspathElement(jar, sourceJar));
                }
            });

            return doc;
        }
    }

    private Element buildClasspathElement(final String jar, final String sourceJar) {
        final Element classpathentry = new Element("classpathentry");
        if (sourceJar != null) {
            classpathentry.addAttribute(new Attribute("sourcepath", sourceJar));
        }
        classpathentry.addAttribute(new Attribute("kind", "lib"));
        classpathentry.addAttribute(new Attribute("path", jar));

        return classpathentry;
    }

}
