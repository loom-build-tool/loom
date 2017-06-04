package jobt.plugin.checkstyle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.ModuleFactory;
import com.puppycrawl.tools.checkstyle.PackageObjectFactory;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

import jobt.api.CompileTarget;
import jobt.api.ExecutionContext;
import jobt.api.Task;
import jobt.api.TaskStatus;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class CheckstyleTask implements Task {

    private final Path baseDir;
    private final CompileTarget compileTarget;
    private final ExecutionContext executionContext;

    private String configLocation = "config/checkstyle/checkstyle.xml";
    private boolean omitIgnoredModules = true;

    public CheckstyleTask(final CompileTarget compileTarget,
                          final ExecutionContext executionContext) {

        this.compileTarget = compileTarget;
        this.executionContext = executionContext;

        switch (compileTarget) {
            case MAIN:
                this.baseDir = Paths.get("src/main/java");
                break;
            case TEST:
                this.baseDir = Paths.get("src/test/java");
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        if (Files.notExists(baseDir)) {
            return TaskStatus.SKIP;
        }

        final List<File> collect = Files.walk(baseDir)
            .filter(Files::isRegularFile)
            .map(Path::toFile)
            .collect(Collectors.toList());

        final LoggingAuditListener listener = new LoggingAuditListener();

        final RootModule checker = createRootModule();
        checker.addListener(listener);

        final int errors = checker.process(collect);
        checker.destroy();

        if (errors == 0) {
            return TaskStatus.OK;
        }

        throw new IllegalStateException();
    }

    private RootModule createRootModule()
        throws MalformedURLException, ExecutionException, InterruptedException {
        final RootModule rootModule;
        final String classpath = "";

        try {
            final Properties props = createOverridingProperties();
            final Configuration config =
                ConfigurationLoader.loadConfiguration(
                    configLocation,
                    new PropertiesExpander(props),
                    omitIgnoredModules);

            final ClassLoader moduleClassLoader =
                Checker.class.getClassLoader();

            final ModuleFactory factory = new PackageObjectFactory(
                Checker.class.getPackage().getName() + ".", moduleClassLoader);

            rootModule = (RootModule) factory.createModule(config.getName());
            rootModule.setModuleClassLoader(moduleClassLoader);

            if (rootModule instanceof Checker) {
                final ClassLoader loader = buildClassLoader();

                ((Checker) rootModule).setClassLoader(loader);
            }

            rootModule.configure(config);
        } catch (final CheckstyleException ex) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                "Unable to create Root Module: "
                    + "configLocation {%s}, classpath {%s}.", configLocation, classpath), ex);
//            throw new BuildException(String.format(Locale.ROOT, "Unable to create Root Module: "
//                + "configLocation {%s}, classpath {%s}.", configLocation, classpath), ex);
        }
        return rootModule;
    }

    private Properties createOverridingProperties() {
        final Properties properties = new Properties();
        properties.setProperty("samedir", "config/checkstyle");
        properties.setProperty("project_loc", "");
        return properties;
    }

    private URLClassLoader buildClassLoader()
        throws MalformedURLException, ExecutionException, InterruptedException {

        final List<URL> classpath;
        switch (compileTarget) {
            case MAIN:
                classpath = executionContext.getCompileClasspath();
                break;
            case TEST:
                classpath = executionContext.getTestClasspath();
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

        return new URLClassLoader(classpath.toArray(new URL[]{}));
    }

}
