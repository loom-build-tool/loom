package jobt.plugin.idea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import jobt.api.BuildConfig;
import jobt.api.ExecutionContext;
import jobt.api.JavaVersion;
import jobt.api.Task;
import jobt.api.TaskStatus;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class IdeaTask implements Task {

    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private Builder parser;

    public IdeaTask(final BuildConfig buildConfig, final ExecutionContext executionContext) {
        this.buildConfig = buildConfig;
        this.executionContext = executionContext;
    }

    private static InputStream readResource(final String resourceName) {
        return IdeaTask.class.getResourceAsStream(resourceName);
    }

    @Override
    public void prepare() throws Exception {
        parser = new Builder();
    }

    @Override
    public TaskStatus run() throws Exception {
        final Path currentDir = Paths.get("");

        final Path fileName = currentDir.toAbsolutePath().getFileName();

        if (fileName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        final Path ideaDirectory = Files.createDirectories(currentDir.resolve(".idea"));

        createEncodingsFile(ideaDirectory);
        createMiscFile(ideaDirectory);

        final String imlFilename = fileName.toString() + ".iml";
        createModulesFile(ideaDirectory, imlFilename);
        createImlFile(currentDir.resolve(imlFilename));

        return TaskStatus.OK;
    }

    private void createEncodingsFile(final Path ideaDirectory) throws IOException {
        final Path encodingsFile = ideaDirectory.resolve("encodings.xml");
        try (final InputStream resourceAsStream = readResource("/encodings.xml")) {
            Files.copy(resourceAsStream, encodingsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void createMiscFile(final Path ideaDirectory) throws IOException, ParsingException {
        final Path miscFile = ideaDirectory.resolve("misc.xml");
        try (final InputStream resourceAsStream = readResource("/misc-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element component = doc.getRootElement().getFirstChildElement("component");

            final String languageLevel;
            final String projectJdkName;
            final int javaPlatformVersion = JavaVersion.ofVersion(
                buildConfig.getConfiguration().getOrDefault("javaPlatformVersion", "8"))
                .getNumericVersion();

            languageLevel = "JDK_1_" + javaPlatformVersion;
            if (javaPlatformVersion < 9) {
                projectJdkName = "1." + javaPlatformVersion;
            } else {
                projectJdkName = "" + javaPlatformVersion;
            }

            component.addAttribute(new Attribute("languageLevel", languageLevel));
            component.addAttribute(new Attribute("project-jdk-name", projectJdkName));

            writeDocumentToFile(miscFile, doc);
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

    private void createModulesFile(final Path ideaDirectory, final String imlFilename)
        throws IOException, ParsingException {

        final Path modulesFile = ideaDirectory.resolve("modules.xml");
        try (final InputStream resourceAsStream = readResource("/modules-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element modules = doc.getRootElement().getFirstChildElement("component")
                .getFirstChildElement("modules");

            final Element module = new Element("module");
            module.addAttribute(new Attribute("fileurl", "file://$PROJECT_DIR$/" + imlFilename));
            module.addAttribute(new Attribute("filepath", "$PROJECT_DIR$/" + imlFilename));
            modules.appendChild(module);

            writeDocumentToFile(modulesFile, doc);
        }
    }

    private void createImlFile(final Path imlFile) throws IOException, ParsingException {
        try (final InputStream resourceAsStream = readResource("/iml-template.xml")) {
            final Document doc = parser.build(resourceAsStream);
            final Element component = doc.getRootElement().getFirstChildElement("component");

            for (final Path path : executionContext.getCompileDependencies()) {
                appendJar(component, null, path.toAbsolutePath().toString());
            }

            for (final Path path : executionContext.getTestDependencies()) {
                appendJar(component, "TEST", path.toAbsolutePath().toString());
            }

            writeDocumentToFile(imlFile, doc);
        }
    }

    private void appendJar(final Element item, final String scope, final String jar) {
        final Element orderEntry = new Element("orderEntry");
        orderEntry.addAttribute(new Attribute("type", "module-library"));
        if (scope != null) {
            orderEntry.addAttribute(new Attribute("scope", scope));
        }

        final Element library = new Element("library");

        final Element classes = new Element("CLASSES");
        final Element root = new Element("root");
        root.addAttribute(new Attribute("url", String.format("jar://%s!/", jar)));
        classes.appendChild(root);
        library.appendChild(classes);

        library.appendChild(new Element("SOURCES"));


        library.appendChild(new Element("JAVADOC"));
        orderEntry.appendChild(library);

        item.appendChild(orderEntry);
    }

}
