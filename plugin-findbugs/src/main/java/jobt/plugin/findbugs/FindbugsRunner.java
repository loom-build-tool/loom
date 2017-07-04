package jobt.plugin.findbugs;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.HTMLBugReporter;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import jobt.api.CompileTarget;
import jobt.api.product.ClasspathProduct;
import jobt.util.Util;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class FindbugsRunner {

    private static final int DEFAULT_PRIORITY_THRESHOLD = Priorities.LOW_PRIORITY;

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsRunner.class);

    private static final String EFFORT_DEFAULT = "default";

    private final CompileTarget compileTarget;
    private final Path sourcesDir;
    private final Path classesDir;
    private final ClasspathProduct classpath;

    private final Optional<Integer> priorityThreshold;

    FindbugsRunner(final CompileTarget compileTarget, final Path sourcesDir, final Path classesDir,
                   final ClasspathProduct classpath, final Optional<Integer> priorityThreshold) {
        this.compileTarget = compileTarget;
        this.sourcesDir = sourcesDir;
        this.classesDir = classesDir;
        this.classpath = classpath;
        this.priorityThreshold = priorityThreshold;
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public void executeFindbugs()
        throws InterruptedException, IOException, NoSuchFieldException, IllegalAccessException {

        prepareEnvironment();

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        final Project project = createFindbugsProject();
        if (project.getFileCount() == 0) {
            LOG.info("Findbugs analysis skipped for this project.");
            return;
        }

        final Integer threshold = priorityThreshold.orElse(DEFAULT_PRIORITY_THRESHOLD);

        final XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
        xmlBugReporter.setPriorityThreshold(threshold);
        xmlBugReporter.setAddMessages(true);
        xmlBugReporter.setOutputStream(new PrintStream(getTargetXMLReport().toFile(), "UTF-8"));

        final HTMLBugReporter htmlBugReporter = new HTMLBugReporter(project, "default.xsl");
        htmlBugReporter.setPriorityThreshold(threshold);
        htmlBugReporter.setOutputStream(new PrintStream(getTargetHTMLReport().toFile(), "UTF-8"));

        final LoggingBugReporter loggingBugReporter = new LoggingBugReporter();
        loggingBugReporter.setPriorityThreshold(threshold);

        final MultiplexingBugReporter multiplexingBugReporter =
            new MultiplexingBugReporter(loggingBugReporter, xmlBugReporter, htmlBugReporter);

        try {
            final FindBugs2 engine = new FindBugs2();

            engine.setProject(project);


            engine.setBugReporter(multiplexingBugReporter);

            engine.setNoClassOk(true);

            final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
            userPreferences.setEffort(EFFORT_DEFAULT);
            engine.setUserPreferences(userPreferences);

            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

            engine.setProgressCallback(new NoOpFindBugsProgress());

            engine.finishSettings();

            engine.execute();

            if (engine.getErrorCount() + engine.getBugCount() > 0) {
                throw new IllegalStateException("Findbugs reported bugs!");
            }
        } finally {
            multiplexingBugReporter.finish();
            System.setSecurityManager(currentSecurityManager);
            Locale.setDefault(initialLocale);
        }
    }

    private void prepareEnvironment() {
        LOG.debug("Prepare/cleanup findbugs environment...");
        try {
            Files.deleteIfExists(getTargetXMLReport());
            Files.createDirectories(FindbugsTask.REPORT_PATH);
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
        LOG.debug("...cleanup done");
    }

    public Path getTargetXMLReport() {
        return FindbugsTask.REPORT_PATH.resolve(String.format("findbugs-%s-result.xml",
            compileTarget.name().toLowerCase()));
    }

    public Path getTargetHTMLReport() {
        return FindbugsTask.REPORT_PATH.resolve(String.format("findbugs-%s-result.html",
            compileTarget.name().toLowerCase()));
    }

    private Project createFindbugsProject() throws IOException {
        final Project findbugsProject = new Project();

        getSourceFiles(sourcesDir).stream()
            .peek(p -> LOG.debug(" +source {}", p))
            .forEach(findbugsProject::addFile);

        getClassesToScan(classesDir).stream()
            .peek(p -> LOG.debug(" +class {}", p))
            .forEach(findbugsProject::addFile);

        classpath.getEntries().stream()
            .map(FindbugsRunner::pathToString)
            .peek(p -> LOG.debug(" +aux {}", p))
            .forEach(findbugsProject::addAuxClasspathEntry);

        if (findbugsProject.getFileList().isEmpty()) {
            throw new IllegalStateException("no source files");
        }

        return findbugsProject;
    }

    private static List<String> getClassesToScan(final Path classesDir) throws IOException {
        return
            Files.walk(classesDir)
                .filter(filterByExtension("class"))
                .map(FindbugsRunner::pathToString)
                .collect(Collectors.toList());
    }

    private static Predicate<Path> filterByExtension(final String extension) {
        Objects.requireNonNull(extension);
        return p -> extension.equals(Util.getFileExtension(p.getFileName().toString()));
    }

    private static String pathToString(final Path file) {
        return file.toAbsolutePath().normalize().toString();
    }

    private static List<String> getSourceFiles(final Path sourcesDir) {
        try {
            return Files.walk(sourcesDir)
                .filter(Files::isRegularFile)
                .filter(filterByExtension("java"))
                .map(FindbugsRunner::pathToString)
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
