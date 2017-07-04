package jobt.plugin.findbugs;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import jobt.api.product.ClasspathProduct;
import jobt.util.Util;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class FindbugsRunner {

    private static final int DEFAULT_PRIORITY_THRESHOLD = Priorities.LOW_PRIORITY;

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsRunner.class);

    private static final String EFFORT_DEFAULT = "default";

    private final Path sourcesDir;
    private final Path classesDir;
    private final ClasspathProduct classpath;

    private final Optional<Integer> priorityThreshold;

    FindbugsRunner(
        final Path sourcesDir,
        final Path classesDir,
        final ClasspathProduct classpath,
        final Optional<Integer> priorityThreshold) {

        this.sourcesDir = sourcesDir;
        this.classesDir = classesDir;
        this.classpath = classpath;

        this.priorityThreshold = priorityThreshold;

    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public List<BugInstance> executeFindbugs() throws InterruptedException {

        prepareEnvironment();

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        try (PrintStream outputStream =
            new PrintStream(getTargetXMLReport().toFile(), "UTF-8")) {
            final FindBugs2 engine = new FindBugs2();
            final Project project = createFindbugsProject();

            if (project.getFileCount() == 0) {
                LOG.info("Findbugs analysis skipped for this project.");
                return Collections.emptyList();
            }

            engine.setProject(project);

            final XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
            xmlBugReporter.setPriorityThreshold(
                priorityThreshold.orElse(DEFAULT_PRIORITY_THRESHOLD));
            xmlBugReporter.setAddMessages(true);
            xmlBugReporter.setOutputStream(outputStream);

            engine.setBugReporter(xmlBugReporter);

            engine.setNoClassOk(true);

            final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
            userPreferences.setEffort(EFFORT_DEFAULT);
            engine.setUserPreferences(userPreferences);

            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

            engine.setProgressCallback(new NoOpFindBugsProgress());

            engine.finishSettings();

            engine.execute();

            final ArrayList<BugInstance> bugs = new ArrayList<>();
            xmlBugReporter.getBugCollection().forEach(bugs::add);
            return bugs;

        } catch (final IOException e) {
            throw new IllegalStateException("Error execution Findbugs", e);
        } finally {
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
        return FindbugsTask.REPORT_PATH.resolve("findbugs-result.xml");
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
