package jobt.plugin.findbugs;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.NoOpFindBugsProgress;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;
import jobt.util.Util;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class FindbugsRunner {

    private static final int DEFAULT_PRIORITY_THRESHOLD = Priorities.LOW_PRIORITY;

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsRunner.class);

    private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

    private static final String EFFORT_DEFAULT = "default";

    private static final CountDownLatch CUSTOM_PLUGINS_INITLATCH = new CountDownLatch(1);

    private final Path sourcesDir;
    private final Path classesDir;
    private final List<URL> auxClasspath;

    private final Optional<Integer> priorityThreshold;

    public FindbugsRunner(
        final Path sourcesDir,
        final Path classesDir,
        final List<URL> auxClasspath,
        final Optional<Integer> priorityThreshold) {

        this.sourcesDir = sourcesDir;
        this.classesDir = classesDir;
        this.auxClasspath = auxClasspath;

        this.priorityThreshold = priorityThreshold;
    }

    public List<BugInstance> findMyBugs() {

        waitForPluginInit();

        try {
            LOG.debug("Prepare/cleanup findbugs environment...");
            prepareEnvironment();
            LOG.debug("...cleanup done");
        } catch (final IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        try (PrintStream outputStream =
            new PrintStream(getTargetXMLReport().toFile(), "UTF-8")) {
            final FindBugs2 engine = new FindBugs2();
            final Project project = createFindbugsProject();

            if (project.getFileCount() == 0) {
                LOG.info("Findbugs analysis skipped for this project.");
                return new ArrayList<>();
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

        } catch (final InterruptedException | IOException e) {
            throw new IllegalStateException("Error execution Findbugs", e);
        } finally {
            System.setSecurityManager(currentSecurityManager);
            Locale.setDefault(initialLocale);
        }
    }

    public static synchronized void startupFindbugsAsync() {
        if (CUSTOM_PLUGINS_INITLATCH.getCount() == 0) {
            return;
        }

        loadFindbugsPlugin();
        disableUpdateChecksOnEveryPlugin();

        LOG.info("Using findbugs custom plugins: {}", Plugin.getAllPluginIds());

        CUSTOM_PLUGINS_INITLATCH.countDown();

    }

    private void prepareEnvironment() throws IOException {
        Files.deleteIfExists(getTargetXMLReport());
        Files.createDirectories(FindbugsTask.REPORT_PATH);
    }

    private static void waitForPluginInit() {
        try {
            CUSTOM_PLUGINS_INITLATCH.await();
        } catch (final InterruptedException e) {
            throw new IllegalStateException("Findbugs Plugin init aborted!", e);
        }

    }

    public Path getTargetXMLReport() {
        return FindbugsTask.REPORT_PATH.resolve("findbugs-result.xml");
    }

    public static String normalizeUrl(final URL url) {
        return url.getPath().split("!")[0].replace("file:", "");
    }

    private Project createFindbugsProject() throws IOException {
        final Project findbugsProject = new Project();

        getSourceFiles(sourcesDir).stream()
            .peek(p -> LOG.debug(" +source {}", p))
            .forEach(findbugsProject::addFile);

        getClassesToScan(classesDir).stream()
            .peek(p -> LOG.debug(" +class {}", p))
            .forEach(findbugsProject::addFile);

        auxClasspath.stream().map(url -> url.getFile())
            .peek(p -> LOG.debug(" +aux {}", p))
            .forEach(findbugsProject::addAuxClasspathEntry);

        if (findbugsProject.getFileList().isEmpty()) {
            throw new RuntimeException("no source files");
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
        return p ->
            Files.isReadable(p)
            && extension.equals(Util.getFileExtension(p.getFileName().toString()));
    }

    private static String pathToString(final Path file) {
        return file.toAbsolutePath().normalize().toString();
    }

    private static List<String> getSourceFiles(final Path sourcesDir) {
        try {
            final List<Path> javaFiles = new ArrayList<>();
            final FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                    if (filterByExtension("java").test(file)) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(sourcesDir, visitor);

            return
                javaFiles.stream()
                .map(FindbugsRunner::pathToString)
                .collect(Collectors.toList());

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Note: findbugs plugins are registered in a static map and thus has many concurrency issues.
     */
    private static List<Plugin> loadFindbugsPlugin() {

        final ClassLoader contextClassLoader = FindbugsRunner.class.getClassLoader();

        try {

            return
            Collections.list(contextClassLoader.getResources("findbugs.xml")).stream()
            .map(url -> FindbugsRunner.normalizeUrl(url))
            .map(Paths::get).filter(p -> Files.exists(p))
            .map(Path::toUri)
            .map(uri -> {
                try {
                    return Optional.of(Plugin.addCustomPlugin(uri, contextClassLoader));
                } catch (final PluginException e) {
                    throw new IllegalStateException("Error loading plugin " + uri, e);
                } catch (final DuplicatePluginIdException e) {
                    if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
                        throw new IllegalStateException(
                            "Duplicate findbugs plugin " + e.getPluginId());
                    }
                    return Optional.<Plugin>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Disable the update check for every plugin. See http://findbugs.sourceforge.net/updateChecking.html
     */
    private static void disableUpdateChecksOnEveryPlugin() {
        for (final Plugin plugin : Plugin.getAllPlugins()) {
            plugin.setMyGlobalOption("noUpdateChecks", "true");
        }
    }

}
