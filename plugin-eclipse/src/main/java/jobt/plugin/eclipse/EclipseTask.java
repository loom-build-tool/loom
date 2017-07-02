package jobt.plugin.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.TaskStatus;
import jobt.api.product.ClasspathProduct;
import jobt.api.product.DummyProduct;
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
    public TaskStatus run() throws Exception {
        final Path currentDir = Paths.get("");

        final Path currentWorkDirName = currentDir.toAbsolutePath().getFileName();

        if (currentWorkDirName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        final String projectName = currentWorkDirName.toString();
        writeDocumentToFile(currentDir.resolve(".project"), createProjectFile(projectName));
        writeDocumentToFile(currentDir.resolve(".classpath"), createClasspathFile());

        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        getProvidedProducts().complete("eclipse", new DummyProduct("Eclipse project files"));
        return status;
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
    private Document createClasspathFile() throws IOException, ParsingException {
        try (final InputStream resourceAsStream = readResource("/classpath-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element root = doc.getRootElement();

            final String projectJdkName;
            final int javaPlatformVersion = buildConfig.getBuildSettings().getJavaPlatformVersion()
                .getNumericVersion();

            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = "" + javaPlatformVersion;
            }

            final Element jdkEntry = new Element("classpathentry");
            jdkEntry.addAttribute(new Attribute("kind", "con"));
            jdkEntry.addAttribute(new Attribute("path",
                String.format("org.eclipse.jdt.launching.JRE_CONTAINER/"
                    + "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s/",
                    projectJdkName)));
            root.appendChild(jdkEntry);

            for (final Path path : getUsedProducts().readProduct("testDependencies",
                ClasspathProduct.class).getEntries()) {
                root.appendChild(buildClasspathElement(path.toAbsolutePath().toString()));
            }

            return doc;
        }
    }

    private Element buildClasspathElement(final String jar) {
        final Element classpathentry = new Element("classpathentry");
//        classpathentry2.addAttribute(new Attribute("sourcepath", "lib"));
        classpathentry.addAttribute(new Attribute("kind", "lib"));
        classpathentry.addAttribute(new Attribute("path", jar));

        return classpathentry;
    }

}
