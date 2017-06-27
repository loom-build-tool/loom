package jobt.plugin.checkstyle;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
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
import com.puppycrawl.tools.checkstyle.XMLLogger;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.RootModule;

import jobt.api.AbstractTask;
import jobt.api.ClasspathProduct;
import jobt.api.CompileTarget;
import jobt.api.ReportProduct;
import jobt.api.SourceTreeProduct;
import jobt.api.TaskStatus;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class CheckstyleTask extends AbstractTask {

    public static final Path REPORT_PATH = Paths.get("jobtbuild", "reports", "checkstyle");

    private static final String CONFIG_LOCATION = "config/checkstyle/checkstyle.xml";
    private static final boolean OMIT_IGNORED_MODULES = true;

    private final CompileTarget compileTarget;

    public CheckstyleTask(final CompileTarget compileTarget) {
        this.compileTarget = compileTarget;
    }

    @Override
    public TaskStatus run() throws Exception {
        final RootModule checker = createRootModule();

        final SourceTreeProduct sourceTree = getSourceTree();

        if (Files.notExists(sourceTree.getSrcDir())) {
            return TaskStatus.SKIP;
        }

        final List<File> collect = Files.walk(sourceTree.getSrcDir())
            .filter(Files::isRegularFile)
            .map(Path::toFile)
            .collect(Collectors.toList());


        final LoggingAuditListener listener = new LoggingAuditListener();
        checker.addListener(listener);

        Files.createDirectories(REPORT_PATH);
        final XMLLogger xmlLogger = new XMLLogger(
            new PrintStream(REPORT_PATH.resolve("checkstyle-report.xml").toFile(), "UTF-8"), true);
        checker.addListener(xmlLogger);

        final int errors = checker.process(collect);
        checker.destroy();

        if (errors == 0) {
            return complete(TaskStatus.OK);
        }

        throw new IllegalStateException("Checkstyle reported errors!");
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("checkstyleMainReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("checkstyleTestReport",
                    new ReportProduct(REPORT_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private SourceTreeProduct getSourceTree() {
        switch (compileTarget) {
            case MAIN:
                return getUsedProducts().readProduct("source", SourceTreeProduct.class);
            case TEST:
                return getUsedProducts().readProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private RootModule createRootModule()
        throws MalformedURLException, ExecutionException, InterruptedException {
        final RootModule rootModule;
        final String classpath = "";

        try {
            final Properties props = createOverridingProperties();
            final Configuration config =
                ConfigurationLoader.loadConfiguration(
                    CONFIG_LOCATION,
                    new PropertiesExpander(props),
                    OMIT_IGNORED_MODULES);

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
                    + "configLocation {%s}, classpath {%s}.", CONFIG_LOCATION, classpath), ex);
//            throw new BuildException(String.format(Locale.ROOT, "Unable to create Root Module: "
//                + "configLocation {%s}, classpath {%s}.", configLocation, classpath), ex);
        }
        return rootModule;
    }

    private Properties createOverridingProperties() {
        final Properties properties = new Properties();

        final Path baseDir = Paths.get("").toAbsolutePath();
        final Path checkstyleConfigDir =
            baseDir.resolve(Paths.get("config", "checkstyle"));

        // Set the same variables as the checkstyle plugin for eclipse
        // http://eclipse-cs.sourceforge.net/#!/properties
        properties.setProperty("basedir", baseDir.toString());
        properties.setProperty("project_loc", baseDir.toString());
        properties.setProperty("samedir", checkstyleConfigDir.toString());
        properties.setProperty("config_loc", checkstyleConfigDir.toString());

        return properties;
    }

    private URLClassLoader buildClassLoader()
        throws MalformedURLException, ExecutionException, InterruptedException {

        final ClasspathProduct classpath;
        switch (compileTarget) {
            case MAIN:
                classpath = getUsedProducts().readProduct(
                    "compileDependencies", ClasspathProduct.class);
                break;
            case TEST:
                classpath = getUsedProducts().readProduct(
                    "testDependencies", ClasspathProduct.class);
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }


        return new URLClassLoader(classpath.getEntriesAsUrlArray());
    }

}
