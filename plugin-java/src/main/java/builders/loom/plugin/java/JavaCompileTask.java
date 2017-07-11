/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.plugin.java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.CompileTarget;
import builders.loom.api.JavaVersion;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.SourceTreeProduct;

public class JavaCompileTask extends AbstractTask {

    public static final Path BUILD_MAIN_PATH = Paths.get("loombuild", "classes", "main");
    public static final Path BUILD_TEST_PATH = Paths.get("loombuild", "classes", "test");

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileTask.class);

    // The first Java version which supports the --release flag
    private static final int JAVA_VERSION_WITH_RELEASE_FLAG = 9;

    private final BuildConfig buildConfig;
    private final RuntimeConfiguration runtimeConfiguration;
    private final CompileTarget compileTarget;
    private final Path buildDir;
    private final String subdirName;
    private final Path cacheDir;

    public JavaCompileTask(final BuildConfig buildConfig,
                           final RuntimeConfiguration runtimeConfiguration,
                           final CompileTarget compileTarget,
                           final Path cacheDir) {
        this.buildConfig = Objects.requireNonNull(buildConfig);
        this.runtimeConfiguration = runtimeConfiguration;
        this.compileTarget = Objects.requireNonNull(compileTarget);
        this.cacheDir = cacheDir;

        switch (compileTarget) {
            case MAIN:
                buildDir = BUILD_MAIN_PATH;
                subdirName = "main";
                break;
            case TEST:
                buildDir = BUILD_TEST_PATH;
                subdirName = "test";
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }
    }

    private static List<String> configuredPlatformVersion(final JavaVersion version) {
        Objects.requireNonNull(version, "versionString required");

        final int parsedJavaSpecVersion = JavaVersion.current().getNumericVersion();
        final int platformVersion = version.getNumericVersion();

        if (platformVersion == parsedJavaSpecVersion) {
            return Collections.emptyList();
        }

        return parsedJavaSpecVersion >= JAVA_VERSION_WITH_RELEASE_FLAG
            ? crossCompileWithReleaseFlag(platformVersion)
            : crossCompileWithSourceTargetFlags(platformVersion);
    }

    private static List<String> crossCompileWithReleaseFlag(final Integer platformVersion) {
        return Arrays.asList("--release", platformVersion.toString());
    }

    private static List<String> crossCompileWithSourceTargetFlags(final Integer platformVersion) {
        return Arrays.asList(
            "-source", platformVersion.toString(),
            "-target", platformVersion.toString(),
            "-bootclasspath", getBootstrapClasspath(),
            "-extdirs", getExtDirs()
        );
    }

    public static String getBootstrapClasspath() {
        return requireEnv("LOOM_JAVA_CROSS_COMPILE_BOOTSTRAPCLASSPATH");
    }

    public static String getExtDirs() {
        return requireEnv("LOOM_JAVA_CROSS_COMPILE_EXTDIRS");
    }

    private static String requireEnv(final String envName) {
        final String env = System.getenv(envName);
        if (env == null) {
            throw new IllegalStateException("System environment variable <"
                + envName + "> not set");
        }
        return env;
    }

    @Override
    public TaskStatus run() throws Exception {
        final List<Path> classpath = new ArrayList<>();

        final Path srcPath;
        switch (compileTarget) {
            case MAIN:
                srcPath = getUsedProducts().readProduct(
                    "source", SourceTreeProduct.class).getSrcDir();
                classpath.add(srcPath);
                classpath.addAll(getUsedProducts().readProduct(
                    "compileDependencies", ClasspathProduct.class).getEntries());
                break;
            case TEST:
                srcPath = getUsedProducts().readProduct(
                    "testSource", SourceTreeProduct.class).getSrcDir();
                classpath.add(srcPath);
                classpath.add(getUsedProducts().readProduct(
                    "compilation", CompilationProduct.class).getClassesDir());
                classpath.addAll(getUsedProducts().readProduct(
                    "testDependencies", ClasspathProduct.class).getEntries());
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

        if (Files.notExists(srcPath)) {
            return complete(TaskStatus.SKIP);
        }

        final List<Path> srcPaths = Files.walk(srcPath)
            .filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".java"))
            .collect(Collectors.toList());

        final FileCacher fileCacher = runtimeConfiguration.isCacheEnabled()
            ? new FileCacherImpl(cacheDir, subdirName) : new NullCacher();

        if (fileCacher.filesCached(srcPaths)) {
            return complete(TaskStatus.UP_TO_DATE);
        }

        if (Files.notExists(buildDir)) {
            Files.createDirectories(buildDir);
        } else {
            FileUtil.deleteDirectoryRecursively(buildDir, false);
        }

        final List<File> srcFiles = srcPaths.stream()
            .map(Path::toFile)
            .collect(Collectors.toList());

        compile(classpath, srcFiles);

        fileCacher.cacheFiles(srcPaths);

        return  complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProduct().complete("compilation", new CompilationProduct(buildDir));
                return status;
            case TEST:
                getProvidedProduct().complete("testCompilation", new CompilationProduct(buildDir));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private void compile(final List<Path> classpath, final List<File> srcFiles) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diagnosticListener =
            diagnostic -> {
                switch (diagnostic.getKind()) {
                    case ERROR:
                        LOG.error(diagnostic.toString());
                        break;
                    case WARNING:
                    case MANDATORY_WARNING:
                        LOG.warn(diagnostic.toString());
                        break;
                    case NOTE:
                        LOG.info(diagnostic.toString());
                        break;
                    default:
                        LOG.debug(diagnostic.toString());
                }
            };

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

            final JavaCompiler.CompilationTask compilerTask = compiler
                .getTask(null, fileManager, diagnosticListener, options, null, compUnits);

            if (!compilerTask.call()) {
                throw new IllegalStateException("Java compile failed");
            }
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

        options.addAll(
            configuredPlatformVersion(buildConfig.getBuildSettings().getJavaPlatformVersion()));

        return options;
    }

}
