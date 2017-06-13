package jobt.plugin.findbugs;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;
import jobt.util.Preconditions;
import jobt.util.Util;

public class FindBugsRunner {

    private static final Logger LOG = LoggerFactory.getLogger(FindBugsRunner.class);


    private static final int BUG_PRIORITY = Priorities.NORMAL_PRIORITY;

    private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

    private final boolean useFbContrib = true;
    private final boolean useFindSecBugs = true;

    public static final String EFFORT_MIN = "min";

    public static final String EFFORT_DEFAULT = "default";

    public static final String EFFORT_MAX = "max";

    private final Path classesDir;
    private final List<URL> auxClasspath;

    private final Iterable<String> sourceFiles;

    private final static CountDownLatch customPluginsInitLatch = new CountDownLatch(1);
    private static List<Plugin> customPlugins = null;


    public static synchronized void initFindBugs() {
        if (customPlugins != null) {
            return;
        }
        Preconditions.checkState(customPluginsInitLatch.getCount() == 1);

        customPlugins = loadFindbugsPlugin();
        disableUpdateChecksOnEveryPlugin();

        LOG.info("Using findbugs custom plugins: {}", Plugin.getAllPluginIds());

        customPluginsInitLatch.countDown();

    }

    public FindBugsRunner(
        final Path sourcesDir,
        final Path classesDir, final List<URL> auxClasspath) {
        this.classesDir = classesDir;
        this.auxClasspath = auxClasspath;

        sourceFiles = getSourceFiles(sourcesDir);
    }

    public List<BugInstance> findMyBugs() {

        waitForPluginInit();

        cleanupPreviousFindbugsRun();

        final SecurityManager currentSecurityManager = System.getSecurityManager();

        // This is a dirty workaround, but unfortunately there is no other way to make Findbugs generate english messages only - see SONARJAVA-380
        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        try (PrintStream outputStream = new PrintStream(getTargetXMLReport().toFile(), "UTF-8")) {
            final FindBugs2 engine = new FindBugs2();
            final Project project = getFindbugsProject();

            if (project.getFileCount() == 0) {
                LOG.info("Findbugs analysis skipped for this project.");
                return new ArrayList<>();
            }

            engine.setProject(project);

            final XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
            xmlBugReporter.setPriorityThreshold(BUG_PRIORITY); // TODO
            xmlBugReporter.setAddMessages(true);
            xmlBugReporter.setOutputStream(outputStream);

            engine.setBugReporter(xmlBugReporter);

            final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
            userPreferences.setEffort(EFFORT_MAX); // TODO make configurable
            engine.setUserPreferences(userPreferences);

            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

            engine.finishSettings();

            engine.execute();

            final ArrayList<BugInstance> bugs = new ArrayList<>();
            xmlBugReporter.getBugCollection().forEach(bugs::add);
            return bugs;

        } catch (final Throwable e) {
            throw new IllegalStateException("Error execution Findbugs", e);
        } finally {
            System.setSecurityManager(currentSecurityManager);
            Locale.setDefault(initialLocale);
        }
    }

    private void cleanupPreviousFindbugsRun() {
        try {
            Files.deleteIfExists(getTargetXMLReport());
        } catch (final IOException e) {
        }
    }


    private static void waitForPluginInit() {
        try {
            customPluginsInitLatch.await();
        } catch (final InterruptedException e) {
            throw new IllegalStateException("Findbugs Plugin init aborted!", e);
        }

    }

    public Path getTargetXMLReport() {
        return FindbugsTask.REPORT_PATH.resolve("findbugs-result.xml");
    }

    public static String normalizeUrl(final URL url) {
        try {
            return StringUtils.removeStart(StringUtils.substringBefore(url.toURI().getSchemeSpecificPart(), "!"), "file:");
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


    public Project getFindbugsProject() throws IOException {
        final Project findbugsProject = new Project();

        for (final String fileName : sourceFiles) { //The original source file are look at by some detectors
                findbugsProject.addFile(fileName);
        }

        Files.walk(classesDir)
            .filter(p -> "class".equals(Util.getFileExtension(p.getFileName().toString())))
            .map(p -> p.toAbsolutePath().normalize())
            .map(Path::toString)
            .forEach(findbugsProject::addFile);

        auxClasspath.stream().map(url -> url.getFile())
            .forEach(findbugsProject::addAuxClasspathEntry);

        if (findbugsProject.getFileList().isEmpty()) {
            throw new RuntimeException("no source files");
        }

        findbugsProject.setCurrentWorkingDirectory(workDir());
        return findbugsProject;
    }

    private File workDir() {
        return Paths.get("work").toFile();
    }

    private static List<String> getSourceFiles(final Path sourcesDir) {
        try {
            return
                Files.list(sourcesDir).filter(p -> Files.isRegularFile(p))
                .filter(p -> "java".equals(Util.getFileExtension(p.getFileName().toString())))
                .filter(p -> !p.getFileName().toString().equals("package-info.java"))
                .map(p -> p.toAbsolutePath().toString())
                .collect(Collectors.toList());

        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * Note: findbugs plugins are registered in a static map and thus has many concurrency issues.
     */
    private static List<Plugin> loadFindbugsPlugin() {

        final ClassLoader contextClassLoader = FindBugsRunner.class.getClassLoader();

        try {

            return
            Collections.list(contextClassLoader.getResources("findbugs.xml")).stream()
            .map(url -> FindBugsRunner.normalizeUrl(url))
            .map(Paths::get).filter(p -> Files.exists(p))
            .map(Path::toUri)
            .map(uri -> {
                try {
                    return Optional.of(Plugin.addCustomPlugin(uri, contextClassLoader));
                } catch (final PluginException e) {
                    throw new IllegalStateException("Error loading plugin " + uri, e);
                } catch (final DuplicatePluginIdException e) {
                    if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
                        throw new IllegalStateException("Duplicate findbugs plugin "+e.getPluginId());
                    }
                    return Optional.<Plugin>empty();
                }
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        } catch (final IOException e) {
            throw new RuntimeException(e); // FIXME
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
