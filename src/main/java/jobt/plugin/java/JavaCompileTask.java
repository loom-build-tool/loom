package jobt.plugin.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.DependencyResolutionException;

import jobt.FileCacher;
import jobt.MavenResolver;
import jobt.Progress;
import jobt.config.BuildConfig;
import jobt.plugin.Task;

public class JavaCompileTask implements Task {

    private final BuildConfig buildConfig;

    public JavaCompileTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public void run() throws Exception {
        final List<File> dependencies = buildClasspath(buildConfig.getDependencies());

        Progress.newStatus("Build Java");
        final FileCacher fileCacher = new FileCacher();

        final Path buildDir = Paths.get("jobtbuild", "build");
        if (Files.notExists(buildDir)) {
            Files.createDirectories(buildDir);
        }

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnosticListener = new DiagnosticCollector<>();

        final List<String> options = buildOptions(buildDir);

        final List<Path> srcFiles = Files.walk(Paths.get("src/main/java"))
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
            Progress.complete("UP-TO-DATE");
        } else {
            final StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnosticListener, null, StandardCharsets.UTF_8);

            fileManager.setLocation(StandardLocation.CLASS_PATH, dependencies);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(buildDir.toFile()));


            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromFiles(nonCachedFiles);

            if (!compiler.getTask(null, fileManager, diagnosticListener,
                options, null, compUnits).call()) {

                for (final Diagnostic<? extends JavaFileObject> diagnostic
                    : diagnosticListener.getDiagnostics()) {
                    Progress.log(diagnostic.toString());
                }

                throw new IllegalStateException("Compile failed");
            }

            fileCacher.cacheFiles(srcFiles);

            Progress.complete();
        }

    }

    private List<String> buildOptions(final Path buildDir) {
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

    private List<File> buildClasspath(final List<String> dependencies)
        throws DependencyCollectionException, DependencyResolutionException, IOException {

        if (dependencies == null || dependencies.isEmpty()) {
            return Collections.emptyList();
        }

        final List<File> classpath = new MavenResolver()
            .buildClasspath(dependencies, "compile");
        classpath.add(new File("src/main/java"));
        return classpath;
    }

}
