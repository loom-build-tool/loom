package jobt.plugin.idea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

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

import jobt.api.BuildConfig;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class IdeaTask implements Task {

    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private DocumentBuilder documentBuilder;
    private Transformer transformer;

    public IdeaTask(final BuildConfig buildConfig, final ExecutionContext executionContext) {
        this.buildConfig = buildConfig;
        this.executionContext = executionContext;
    }

    private static InputStream readResource(final String resourceName) {
        return IdeaTask.class.getResourceAsStream(resourceName);
    }

    @Override
    public void prepare() throws Exception {
        documentBuilder = newDocumentBuilder();
        transformer = newTransformer();
    }

    private DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private Transformer newTransformer() throws TransformerConfigurationException {
        final Transformer newTransformer = TransformerFactory.newInstance().newTransformer();
        newTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
        newTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return newTransformer;
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
        createImlFile(ideaDirectory, imlFilename);

        return TaskStatus.OK;
    }

    private void createEncodingsFile(final Path ideaDirectory) throws IOException {
        final Path encodingsFile = ideaDirectory.resolve("encodings.xml");
        try (final InputStream resourceAsStream = readResource("/encodings.xml")) {
            Files.copy(resourceAsStream, encodingsFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void createMiscFile(final Path ideaDirectory)
        throws IOException, SAXException, TransformerException {

        final Path miscFile = ideaDirectory.resolve("misc.xml");
        try (final InputStream resourceAsStream = readResource("/misc.xml")) {
            final Document doc = documentBuilder.parse(resourceAsStream);
            final Element component = (Element) doc.getElementsByTagName("component").item(0);

            final String languageLevel;
            final String projectJdkName;
            final int javaPlatformVersion = parseJavaVersion(buildConfig.getConfiguration()
                .getOrDefault("javaPlatformVersion", "8"));
            switch (javaPlatformVersion) {
                case 5:
                case 6:
                case 7:
                case 8:
                    languageLevel = "JDK_1_" + javaPlatformVersion;
                    projectJdkName = "1." + javaPlatformVersion;
                    break;
                case 9:
                    languageLevel = "JDK_1_9";
                    projectJdkName = "9";
                    break;
                default:
                    throw new IllegalStateException("Unsupported javaPlatformVersion: "
                        + javaPlatformVersion);
            }

            component.setAttribute("languageLevel", languageLevel);
            component.setAttribute("project-jdk-name", projectJdkName);

            doc.setXmlStandalone(true);
            writeDocumentToFile(miscFile, doc);
        }
    }

    private void writeDocumentToFile(final Path file, final Document doc)
        throws IOException, TransformerException {

        try (final OutputStream outputStream = Files.newOutputStream(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            transformer.transform(new DOMSource(doc), new StreamResult(outputStream));
        }
    }

    private void createModulesFile(final Path ideaDirectory, final String imlFilename)
        throws IOException, SAXException, TransformerException {

        final Path modulesFile = ideaDirectory.resolve("modules.xml");
        try (final InputStream resourceAsStream = readResource("/modules-template.xml")) {
            final Document doc = documentBuilder.parse(resourceAsStream);
            final Node modules = doc.getElementsByTagName("modules").item(0);

            final Element module = doc.createElement("module");
            module.setAttribute("fileurl", "file://$PROJECT_DIR$/" + imlFilename);
            module.setAttribute("filepath", "$PROJECT_DIR$/" + imlFilename);
            modules.appendChild(module);

            doc.setXmlStandalone(true);

            writeDocumentToFile(modulesFile, doc);
        }
    }

    private void createImlFile(final Path ideaDirectory, final String imlFilename)
        throws IOException, SAXException, TransformerException {

        final Path imlFile = ideaDirectory.resolve(imlFilename);
        try (final InputStream resourceAsStream = readResource("/iml-template.xml")) {
            final Document doc = documentBuilder.parse(resourceAsStream);
            final Node component = doc.getElementsByTagName("component").item(0);

            for (final Path path : executionContext.getCompileDependencies()) {
                appendJar(doc, component, null, path.toAbsolutePath().toString());
            }

            for (final Path path : executionContext.getTestDependencies()) {
                appendJar(doc, component, "TEST", path.toAbsolutePath().toString());
            }

            doc.setXmlStandalone(true);

            writeDocumentToFile(imlFile, doc);
        }
    }

    private void appendJar(final Document doc, final Node item, final String scope,
                           final String jar) {

        final Element orderEntry = doc.createElement("orderEntry");
        orderEntry.setAttribute("type", "module-library");
        if (scope != null) {
            orderEntry.setAttribute("scope", scope);
        }

        final Element library = doc.createElement("library");
        final Element classes = doc.createElement("CLASSES");
        final Element root = doc.createElement("root");
        root.setAttribute("url", String.format("jar://%s!/", jar));
        classes.appendChild(root);
        library.appendChild(doc.createElement("JAVADOC"));
        library.appendChild(doc.createElement("SOURCES"));
        library.appendChild(classes);
        orderEntry.appendChild(library);

        item.appendChild(orderEntry);
    }

    @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:returncount"})
    private static int parseJavaVersion(final String javaVersion) {
        switch (javaVersion) {
            case "9":
                return 9;
            case "8":
            case "1.8":
                return 8;
            case "7":
            case "1.7":
                return 7;
            case "6":
            case "1.6":
                return 6;
            case "5":
            case "1.5":
                return 5;
            default:
                throw new IllegalStateException("Java Platform Version <" + javaVersion + "> is "
                    + "unsupported");
        }
    }

}
