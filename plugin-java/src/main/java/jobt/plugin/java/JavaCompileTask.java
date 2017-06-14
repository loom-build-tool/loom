package jobt.plugin.java;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.DependencyResolver;
import jobt.api.ExecutionContext;
import jobt.api.RuntimeConfiguration;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class JavaCompileTask implements Task {

    public static final Path SRC_MAIN_PATH = Paths.get("src/main/java");
    public static final Path SRC_TEST_PATH = Paths.get("src/test/java");
    public static final Path BUILD_MAIN_PATH = Paths.get("jobtbuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("jobtbuild", "classes", "test");

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileTask.class);
    private static final int DEFAULT_JAVA_PLATFORM = 8;

    // The first Java version which supports the --release flag
    private static final int JAVA_VERSION_WITH_RELEASE_FLAG = 9;

    private final BuildConfig buildConfig;
    private final RuntimeConfiguration runtimeConfiguration;
    private final ExecutionContext executionContext;
    private final CompileTarget compileTarget;
    private final Path srcPath;
    private final Path buildDir;
    private final String subdirName;
    private final List<Path> classpathAppendix = new ArrayList<>();
    private final DependencyResolver dependencyResolver;
    private final List<String> dependencies;
    private final String dependencyScope;

    private Future<List<Path>> resolvedDependencies;

    public JavaCompileTask(final BuildConfig buildConfig,
                           final RuntimeConfiguration runtimeConfiguration,
                           final ExecutionContext executionContext,
                           final CompileTarget compileTarget,
                           final DependencyResolver dependencyResolver) {
        this.buildConfig = Objects.requireNonNull(buildConfig);
        this.runtimeConfiguration = runtimeConfiguration;
        this.executionContext = Objects.requireNonNull(executionContext);
        this.compileTarget = Objects.requireNonNull(compileTarget);
        this.dependencyResolver = Objects.requireNonNull(dependencyResolver);

        dependencies = new ArrayList<>();
        if (buildConfig.getDependencies() != null) {
            dependencies.addAll(buildConfig.getDependencies());
        }

        switch (compileTarget) {
            case MAIN:
                srcPath = SRC_MAIN_PATH;
                buildDir = BUILD_MAIN_PATH;
                subdirName = "main";
                dependencyScope = "compile";
                classpathAppendix.add(srcPath);
                break;
            case TEST:
                srcPath = SRC_TEST_PATH;
                buildDir = BUILD_TEST_PATH;
                subdirName = "test";
                dependencyScope = "test";
                classpathAppendix.add(srcPath);
                classpathAppendix.add(BUILD_MAIN_PATH);

                if (buildConfig.getTestDependencies() != null) {
                    dependencies.addAll(buildConfig.getTestDependencies());
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public void prepare() throws Exception {
        resolvedDependencies =
            dependencyResolver.resolveDependencies(dependencies, dependencyScope);
    }

    @Override
    public TaskStatus run() throws Exception {
        if (Files.notExists(srcPath)) {
            return TaskStatus.SKIP;
        }

        final List<Path> classpath = new ArrayList<>(resolvedDependencies.get());
        classpath.addAll(classpathAppendix);

        final List<URL> urls = classpath.stream()
            .map(JavaCompileTask::buildUrl)
            .collect(Collectors.toList());

        switch (compileTarget) {
            case MAIN:
                executionContext.setCompileClasspath(urls);
                break;
            case TEST:
                executionContext.setTestClasspath(urls);
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

        final List<Path> srcPaths = Files.walk(srcPath)
            .filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".java"))
            .collect(Collectors.toList());

        final FileCacher fileCacher = runtimeConfiguration.isCacheEnabled()
            ? new FileCacher(subdirName) : null;

        if (fileCacher != null && fileCacher.filesCached(srcPaths)) {
            return TaskStatus.UP_TO_DATE;
        }

        if (Files.notExists(buildDir)) {
            Files.createDirectories(buildDir);
        } else {
            FileUtil.deleteDirectoryRecursively(buildDir, false);
        }

        final List<File> srcFiles = srcPaths.stream()
            .map(Path::toFile)
            .collect(Collectors.toList());

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocation(StandardLocation.CLASS_PATH,
                classpath.stream().map(Path::toFile).collect(Collectors.toList()));
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(buildDir.toFile()));

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromFiles(srcFiles);

            final List<String> options = buildOptions();

            LOG.info("Compile {} sources with options {}", srcFiles.size(), options);

            if (!compiler.getTask(null, fileManager, diagnosticListener,
                options, null, compUnits).call()) {

                final String collect = diagnosticListener.getDiagnostics().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));

                throw new IllegalStateException(collect);
            }
        }

        if (fileCacher != null) {
            fileCacher.cacheFiles(srcPaths);
        }

        return TaskStatus.OK;
    }

    private static URL buildUrl(final Path f) {
        try {
            return f.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<String> buildOptions() {
        final List<String> options = new ArrayList<>();
        options.add("-d");
        options.add(buildDir.toString());

        options.add("-encoding");
        options.add("UTF-8");

        options.add("-Xlint:all");

        options.add("-sourcepath");
        options.add("");

        options.add("-Xpkginfo:always");

        final Map<String, String> config = buildConfig.getConfiguration();

        options.addAll(configuredPlatformVersion(config.get("javaPlatformVersion")));

        return options;
    }

    private List<String> configuredPlatformVersion(final String versionString) {
        final String javaSpecVersion = System.getProperty("java.specification.version");

        if (javaSpecVersion == null) {
            throw new IllegalStateException("Unknown java.specification.version");
        }

        final int parsedJavaSpecVersion = parseJavaVersion(javaSpecVersion);

        final int platformVersion =
            Optional.ofNullable(versionString)
                .map(JavaCompileTask::parseJavaVersion)
                .orElse(DEFAULT_JAVA_PLATFORM);

        if (platformVersion == parsedJavaSpecVersion) {
            return Collections.emptyList();
        }

        return parsedJavaSpecVersion >= JAVA_VERSION_WITH_RELEASE_FLAG
            ? crossCompileWithReleaseFlag(platformVersion)
            : crossCompileWithSourceTargetFlags(platformVersion);
    }

    @SuppressWarnings({ "checkstyle:magicnumber", "checkstyle:returncount" })
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

    private List<String> crossCompileWithReleaseFlag(final Integer platformVersion) {
        return Arrays.asList("--release", platformVersion.toString());
    }

    private List<String> crossCompileWithSourceTargetFlags(final Integer platformVersion) {
        return Arrays.asList(
            "-source", platformVersion.toString(),
            "-target", platformVersion.toString(),
            "-bootclasspath", getBootstrapClasspath(),
            "-extdirs", getExtDirs()
        );
    }

    public String getBootstrapClasspath() {
        return requireEnv("JOBT_JAVA_CROSS_COMPILE_BOOTSTRAPCLASSPATH");
    }

    public String getExtDirs() {
        return requireEnv("JOBT_JAVA_CROSS_COMPILE_EXTDIRS");
    }

    private String requireEnv(final String envName) {
        final String env = System.getenv(envName);
        if (env == null) {
            throw new IllegalStateException("System environment variable <"
                + envName + "> not set");
        }
        return env;
    }

}
