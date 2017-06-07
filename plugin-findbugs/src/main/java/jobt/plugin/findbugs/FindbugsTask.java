package jobt.plugin.findbugs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.DependencyResolver;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.util.Util;


public class FindbugsTask implements Task {

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsTask.class);

    private final ClassLoader extClassLoader;
    private final ExecutionContext executionContext;
    private final DependencyResolver dependencyResolver;

    public FindbugsTask(final ClassLoader extClassLoader,
        final ExecutionContext executionContext, final DependencyResolver dependencyResolver) {
        this.extClassLoader = extClassLoader;
        this.executionContext = executionContext;
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
//        final Classpath resolvedBuildDependencies = executionContext.lookup(Classpath.class, "compileJava.resolvedBuildDependencies");
        //        final Classpath classpathTestBuildDeps = executionContext.lookup(Classpath.class, ZZZZZ);
        final Classpath compileOutput = new Classpath(
            Collections.singletonList(Paths.get("/Users/sostermayr/workspace_braincode/jobt-example/jobtbuild/classes")));

        //        final Classpath classpathSrcTest = executionContext.lookup(Classpath.class, ZZZZ);

        //    runFindBugs.run();
        // TODO inject
//        final MavenResolver mavenResolver = new MavenResolver();

        // TODO configure, preload

        final List<Path> fbDependencies =
            dependencyResolver.resolveDependencies(
                Collections.singletonList("com.google.code.findbugs:findbugs:3.0.1"), "compile")
            .get();

        System.out.println("fbDependencies=");
        fbDependencies.forEach(System.out::println);


        final URL[] fbUrls = fbDependencies.stream()
            .map(Util::toUrl)
            .toArray(URL[]::new);

        final String checkTarget = compileOutput.getSingleEntry().toString();
        System.out.println("checkTarget="+checkTarget);

        FindBugsInvoker.runFindBugs();

//        final URLClassLoader urlClassLoader = ClassLoaderUtils.createUrlClassLoader(fbUrls, extClassLoader);
//        ClassLoaderUtils.debug(urlClassLoader);
//        final ClassLoader backup = Thread.currentThread().getContextClassLoader();

//        Thread.currentThread().setContextClassLoader(urlClassLoader);
//        try {
//            runFindBugs(urlClassLoader);
//        } finally {
//            Thread.currentThread().setContextClassLoader(backup);
//        }

//        Dispatcher.executeInWorkerClassLoader(urlClassLoader, FindBugsInvoker.class, new FindBugsArgs());


        return TaskStatus.OK;
    }

    private static void runFindBugs(final ClassLoader classLoader)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException,
        NoSuchMethodException, InvocationTargetException {
        final Class<?> findBugsClass = classLoader.loadClass("edu.umd.cs.findbugs.FindBugs");
        final Class<?> findBugs2Class = classLoader.loadClass("edu.umd.cs.findbugs.FindBugs2");
        final Class<?> textUICommandLineClass = classLoader.loadClass("edu.umd.cs.findbugs.TextUICommandLine");

//            final FindBugs2 findBugs2 = new FindBugs2();
        final Object findBugs2 = findBugs2Class.newInstance();

//            final TextUICommandLine commandLine = new TextUICommandLine();
        final Object commandLine = textUICommandLineClass.newInstance();

        final Path resultFile = Paths.get("findbugs-results.txt");
        Files.deleteIfExists(resultFile);
        final String classpathArg = formatClasspath(Collections.emptyList());
        final String[] strArray = new String[] {"-output","findbugs-results.txt",
            "-auxclasspath",
            classpathArg,
//                "jobtbuild/classes/main/"
            "jobt-findbugs-sample/build/classes/main"
            };
//            FindBugs.processCommandLine(commandLine, strArray, findBugs2);
//            findBugs2.execute();

        final Method processCommandLine = findBugsClass.getDeclaredMethod(
          "processCommandLine", textUICommandLineClass, String[].class, classLoader.loadClass("edu.umd.cs.findbugs.IFindBugsEngine"));
        processCommandLine.invoke(null, commandLine, strArray, findBugs2);

        findBugs2Class.getMethod("execute").invoke(findBugs2);

        System.out.println("INIT DONE");

        final List<String> resultLines = Files.readAllLines(resultFile);
        System.out.println("===RESULTS===");
        resultLines.forEach(System.out::println);
        System.out.println("=============");
    }


    private static String formatClasspath(final List<Classpath> auxclasspaths) {

        return auxclasspaths.stream()
            .flatMap(cps -> cps.getEntries().stream())
            .map(file -> file.toString())
            .collect(Collectors.joining(":"));
    }



}
