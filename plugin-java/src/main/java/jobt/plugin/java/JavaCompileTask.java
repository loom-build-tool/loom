package jobt.plugin.java;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import jobt.api.Task;
import jobt.api.TaskStatus;

public class JavaCompileTask implements Task {

    public static final Path SRC_MAIN_PATH = Paths.get("src/main/java");
    public static final Path SRC_TEST_PATH = Paths.get("src/test/java");
    public static final Path BUILD_MAIN_PATH = Paths.get("jobtbuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("jobtbuild", "classes", "test");

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileTask.class);

    private final BuildConfig buildConfig;
    private final ExecutionContext executionContext;
    private final CompileTarget compileTarget;
    private final Path srcPath;
    private final Path buildDir;
    private final List<Path> classpathAppendix = new ArrayList<>();
    private final DependencyResolver dependencyResolver;
    private final List<String> dependencies;
    private final String dependencyScope;

    private Future<List<Path>> resolvedDependencies;

    public JavaCompileTask(final BuildConfig buildConfig,
                           final ExecutionContext executionContext,
                           final CompileTarget compileTarget,
                           final DependencyResolver dependencyResolver) {
        this.buildConfig = Objects.requireNonNull(buildConfig);
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
                dependencyScope = "compile";
                classpathAppendix.add(srcPath);
                break;
            case TEST:
                srcPath = SRC_TEST_PATH;
                buildDir = BUILD_TEST_PATH;
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
            .map(JavaCompileTask::buildUrl).collect(Collectors.toList());

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

        final FileCacher fileCacher = new FileCacher();

        if (Files.notExists(buildDir)) {
            Files.createDirectories(buildDir);
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();

        final List<String> options = buildOptions();

        final List<Path> srcFiles = Files.walk(srcPath)
            .filter(f -> Files.isRegularFile(f))
            .filter(f -> f.toString().endsWith(".java"))
            .collect(Collectors.toList());

        final List<File> nonCachedFiles;
        if (fileCacher.isCacheEmpty()) {
            nonCachedFiles = srcFiles.stream()
                .map(Path::toFile)
                .collect(Collectors.toList());
        } else {
            nonCachedFiles = srcFiles.stream()
                .filter(fileCacher::notCached)
                .map(Path::toFile)
                .collect(Collectors.toList());
        }

        if (nonCachedFiles.isEmpty()) {
            return TaskStatus.UP_TO_DATE;
        }

        final StandardJavaFileManager fileManager =
            compiler.getStandardFileManager(diagnosticListener, null, StandardCharsets.UTF_8);

        fileManager.setLocation(StandardLocation.CLASS_PATH,
            classpath.stream().map(Path::toFile).collect(Collectors.toList()));
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
            Collections.singletonList(buildDir.toFile()));

        final Iterable<? extends JavaFileObject> compUnits =
            fileManager.getJavaFileObjectsFromFiles(nonCachedFiles);

        if (!compiler.getTask(null, fileManager, diagnosticListener,
            options, null, compUnits).call()) {

            final String collect = diagnosticListener.getDiagnostics().stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));

            throw new IllegalStateException(collect);
        }

        fileCacher.cacheFiles(srcFiles);

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

        final Map<String, String> config = buildConfig.getConfiguration();
        final String sourceCompatibility = config.get("sourceCompatibility");
        if (sourceCompatibility != null) {
            options.add("-source");
            options.add(sourceCompatibility);
        }

        final String targetCompatibility = config.get("targetCompatibility");
        if (targetCompatibility != null) {
            options.add("-target");
            options.add(targetCompatibility);
        }

        options.add("-bootclasspath");
        options.add(buildRuntimePath());

        options.add("-Xlint:all");

        return options;
    }

    private String buildRuntimePath() {
        return Paths.get(System.getProperty("java.home"), "jre/lib/rt.jar").toString();
    }

}
