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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.XMLBugReporter;
import edu.umd.cs.findbugs.config.UserPreferences;
import jobt.util.Preconditions;
import jobt.util.Util;

public class FindBugsSonarWayMain {

    private static final String USERS_SOSTERMAYR_WORKSPACE_BRAINCODE_JOBT_EXAMPLE = "/Users/sostermayr/workspace_braincode/jobt-example";

    private static final Logger LOG = LoggerFactory.getLogger(FindBugsSonarWayMain.class);

    private final boolean useFbContrib = true;
    private final boolean useFindSecBugs = true;

    private static Path baseDir = Paths.get(USERS_SOSTERMAYR_WORKSPACE_BRAINCODE_JOBT_EXAMPLE);

    public static final String EFFORT_MIN = "min";

    public static final String EFFORT_DEFAULT = "default";

    public static final String EFFORT_MAX = "max";

    private final Path classesDir;
    public FindBugsSonarWayMain(final Path classesDir) {
        this.classesDir = classesDir;
    }

    public static void main(final String[] args) {
        Preconditions.checkState(Files.isDirectory(baseDir), "missing basedir "+baseDir);


        final List<BugInstance> bugs = new FindBugsSonarWayMain(baseDir.resolve(Paths.get("jobtbuild/classes/main"))).findMyBugs();;

        for (final BugInstance bug : bugs) {

                System.out.println("bug #"+bug.getMessage());

        }
    }

    public List<BugInstance> findMyBugs() {

        System.out.println("NEW STYLE INVOKER @ work");

        // We keep a handle on the current security manager because FB plays with it and we need to restore it before shutting down the executor
        // service
//        final SecurityManager currentSecurityManager = System.getSecurityManager();
//        final ClassLoader initialClassLoader = Thread.currentThread().getContextClassLoader();
//        Thread.currentThread().setContextClassLoader(FindBugs2.class.getClassLoader());

        // This is a dirty workaround, but unfortunately there is no other way to make Findbugs generate english messages only - see SONARJAVA-380
        final Locale initialLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);

        System.out.println("CHECK");

        final Collection<Plugin> customPlugins = null;
        try (PrintStream outputStream = new PrintStream(getTargetXMLReport(), "UTF-8")) {
            System.out.println("CHECK");
            final FindBugs2 engine = new FindBugs2();
            System.out.println("CHECK");
            final Project project = getFindbugsProject();
            System.out.println("CHECK");

            //            if(project.getFileCount() == 0) {
            //                LOG.info("Findbugs analysis skipped for this project.");
            //                return new ArrayList<>();
            //            }

            //            customPlugins = loadFindbugsPlugins(useFbContrib,useFindSecBugs);

            disableUpdateChecksOnEveryPlugin();
            System.out.println("CHECK");
            engine.setProject(project);

            final XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
            xmlBugReporter.setPriorityThreshold(Priorities.NORMAL_PRIORITY); // TODO
            xmlBugReporter.setAddMessages(true);
            System.out.println("CHECK");
            xmlBugReporter.setOutputStream(outputStream);

            engine.setBugReporter(xmlBugReporter);

            final UserPreferences userPreferences = UserPreferences.createDefaultUserPreferences();
            userPreferences.setEffort(EFFORT_MAX); // TODO make configurable
            engine.setUserPreferences(userPreferences);

            // FIXME what is this?
//            engine.addFilter(configuration.saveIncludeConfigXml().getAbsolutePath(), true);

            // TODO excludes
//            for (final File filterFile : configuration.getExcludesFilters()) {
//                if (filterFile.isFile()) {
//                    LOG.info("Use filter-file: {}", filterFile);
//                    engine.addFilter(filterFile.getAbsolutePath(), false);
//                } else {
//                    LOG.warn("FindBugs filter-file not found: {}", filterFile);
//                }
//            }

            engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
            engine.setAnalysisFeatureSettings(FindBugs.DEFAULT_EFFORT);

            engine.finishSettings();

            //            executorService.submit(new FindbugsTask(engine)).get(configuration.getTimeout(), TimeUnit.MILLISECONDS);
            System.out.println("PRE EXEC");
            engine.execute();
            System.out.println("POSt EXEC");

            final ArrayList<BugInstance> bugs = new ArrayList<>();
            xmlBugReporter.getBugCollection().forEach(bugs::add);
            return bugs;
            //            return toReportedBugs(xmlBugReporter.getBugCollection());
        } catch (final Throwable e) {
            System.out.println("FINDBUGS expection ...");
            throw new IllegalStateException("Can not execute Findbugs", e);
        } finally {
            // we set back the original security manager BEFORE shutting down the executor service, otherwise there's a problem with Java 5
//            System.setSecurityManager(currentSecurityManager);
            //            resetCustomPluginList(customPlugins);
//            Thread.currentThread().setContextClassLoader(initialClassLoader);
            Locale.setDefault(initialLocale);
        }
    }

    public File getTargetXMLReport() {
        return Paths.get("findbugs-result.xml").toFile();
    }

    //    private Collection<Plugin> loadFindbugsPlugins(boolean useFbContrib,boolean useFindSecBugs) {
    //        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    //
    //        List<String> pluginJarPathList = Lists.newArrayList();
    //        try {
    //            Enumeration<URL> urls = contextClassLoader.getResources("findbugs.xml");
    //            while (urls.hasMoreElements()) {
    //                URL url = urls.nextElement();
    //                pluginJarPathList.add(normalizeUrl(url));
    //            }
    //            //Add fb-contrib plugin.
    //            if (useFbContrib && configuration.getFbContribJar() != null) {
    //                // fb-contrib plugin is packaged by Maven. It is not available during execution of unit tests.
    //                pluginJarPathList.add(configuration.getFbContribJar().getAbsolutePath());
    //            }
    //            //Add find-sec-bugs plugin. (same as fb-contrib)
    //            if (useFindSecBugs && configuration.getFindSecBugsJar() != null) {
    //                pluginJarPathList.add(configuration.getFindSecBugsJar().getAbsolutePath());
    //            }
    //        } catch (IOException e) {
    //            throw new IllegalStateException(e);
    //        } catch (URISyntaxException e) {
    //            throw new IllegalStateException(e);
    //        }
    //        List<Plugin> customPluginList = Lists.newArrayList();
    //
    //        for (String path : pluginJarPathList) {
    //            try {
    //                Plugin plugin = Plugin.addCustomPlugin(new File(path).toURI(), contextClassLoader);
    //                if (plugin != null) {
    //                    customPluginList.add(plugin);
    //                    LOG.info("Loading findbugs plugin: " + path);
    //                }
    //            } catch (PluginException e) {
    //                LOG.warn("Failed to load plugin for custom detector: " + path);
    //                LOG.debug("Cause of failure", e);
    //            } catch (DuplicatePluginIdException e) {
    //                // FB Core plugin is always loaded, so we'll get an exception for it always
    //                if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
    //                    // log only if it's not the FV Core plugin
    //                    LOG.debug("Plugin already loaded: exception ignored: " + e.getMessage(), e);
    //                }
    //            }
    //        }
    //
    //        return customPluginList;
    //    }
//    private static Collection<ReportedBug> toReportedBugs(final BugCollection bugCollection) {
//        // We need to retrieve information such as the message before we shut everything down as we will lose any custom
//        // bug messages
//        final Collection<ReportedBug> bugs = new ArrayList<ReportedBug>();
//
//        for (final BugInstance bugInstance : bugCollection) {
//            if (bugInstance.getPrimarySourceLineAnnotation() == null) {
//                LOG.warn("No source line for " + bugInstance.getType());
//                continue;
//            }
//
//            bugs.add(new ReportedBug(bugInstance));
//        }
//        return bugs;
//
//    }


    private String normalizeUrl(final URL url) throws URISyntaxException {
        return StringUtils.removeStart(StringUtils.substringBefore(url.toURI().getSchemeSpecificPart(), "!"), "file:");
    }


    public Project getFindbugsProject() throws IOException {
        final Project findbugsProject = new Project();

        for (final File file : getSourceFiles()) { //The original source file are look at by some detectors
            if("java".equals(Util.getFileExtension(file.getName()))) {
                findbugsProject.addFile(file.getCanonicalPath());
            }
        }

//        final List<File> classFilesToAnalyze = new ArrayList<>(javaResourceLocator.classFilesToAnalyze());
//        final List<File> classFilesToAnalyze = new ArrayList<>();

//        for (final File file : javaResourceLocator.classpath()) {
//            //Will capture additional classes including precompiled JSP
//            if(file.isDirectory()) { // will include "/target/classes" and other non-standard folders
//                classFilesToAnalyze.addAll(scanForAdditionalClasses(file));
//            }
//
//            //Auxiliary dependencies
//            findbugsProject.addAuxClasspathEntry(file.getCanonicalPath());
//        }

//        classFilesToAnalyze.addAll(classFiles);

//        final boolean hasJspFiles = fileSystem.hasFiles(fileSystem.predicates().hasLanguage("jsp"));
//        boolean hasPrecompiledJsp = false;
//        for (final File classToAnalyze : classFilesToAnalyze) {
//            final String absolutePath = classToAnalyze.getCanonicalPath();
//            if(hasJspFiles && !hasPrecompiledJsp
//                && (absolutePath.endsWith("_jsp.class") || //Jasper
//                    absolutePath.contains("/jsp_servlet/")) //WebLogic
//                ) {
//                hasPrecompiledJsp = true;
//            }
//            findbugsProject.addFile(absolutePath);
//        }

        // TODO
//        final Path classesDir = baseDir.resolve(Paths.get("jobtbuild/classes/main"));

        Files.walk(classesDir)
            .filter(p -> "class".equals(Util.getFileExtension(p.getFileName().toString())))
            .map(p -> p.toAbsolutePath().normalize())
            .map(Path::toString)
            .forEach(findbugsProject::addFile);

        Files.list(baseDir.resolve("deps"))
            .filter(p -> "jar".equals(Util.getFileExtension(p.getFileName().toString())))
            .map(p -> p.toAbsolutePath().normalize())
            .map(Path::toString)
            .forEach(findbugsProject::addAuxClasspathEntry);

        System.out.println("configure files to "+findbugsProject.getFileList());
        System.out.println("configure auxpath to "+findbugsProject.getAuxClasspathEntryList());


        if (findbugsProject.getFileList().isEmpty()) {
            throw new RuntimeException("no source files");
        }
//
//        if (hasJspFiles && !hasPrecompiledJsp) {
//            LOG.warn("JSP files were found in the current (sub)project ({}) but FindBugs requires their precompiled form. " +
//                "For more information on how to configure JSP precompilation : https://github.com/find-sec-bugs/find-sec-bugs/wiki/JSP-precompilation",
//                fileSystem.baseDir().getPath());
//        }

//        copyLibs();
//        if (annotationsLib != null) {
//            // Findbugs dependencies are packaged by Maven. They are not available during execution of unit tests.
//            findbugsProject.addAuxClasspathEntry(annotationsLib.getCanonicalPath());
//            findbugsProject.addAuxClasspathEntry(jsr305Lib.getCanonicalPath());
//        }
        findbugsProject.setCurrentWorkingDirectory(workDir());
        return findbugsProject;
    }

    private File workDir() {
        return Paths.get("work").toFile();
    }

//    File saveIncludeConfigXml() throws IOException {
//      StringWriter conf = new StringWriter();
//      exporter.exportProfile(profile, conf);
//      File file = new File(fileSystem.workDir(), "findbugs-include.xml");
//      FileUtils.write(file, conf.toString(), CharEncoding.UTF_8);
//      return file;
//    }

    private Iterable<File> getSourceFiles() {
        try {
            // FIXME
            return
                Files.list(baseDir.resolve(Paths.get("src/main/java")) ).filter(p -> Files.isRegularFile(p))
                .filter(p -> "java".equals(Util.getFileExtension(p.getFileName().toString())))
                .map(Path::toFile).collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        //        FilePredicates pred = fileSystem.predicates();
        //        return fileSystem.files(pred.and(
        //                pred.hasType(Type.MAIN),
        //                pred.hasLanguage(Java.KEY),
        //                pred.not(pred.matchesPathPattern("**/package-info.java"))
        //        ));
    }

    /**
     * Disable the update check for every plugin. See http://findbugs.sourceforge.net/updateChecking.html
     */
    private void disableUpdateChecksOnEveryPlugin() {
        for (final Plugin plugin : Plugin.getAllPlugins()) {
            plugin.setMyGlobalOption("noUpdateChecks", "true");
        }
    }

    private void resetCustomPluginList(final Collection<Plugin> customPlugins) {
        if (customPlugins != null) {
            for (final Plugin plugin : customPlugins) {
                Plugin.removeCustomPlugin(plugin);
            }
        }
    }

}
