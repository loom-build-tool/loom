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
import java.util.Optional;
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
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.SourceTreeProduct;

public class JavaCompileTask extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileTask.class);

    // The first Java version which supports the --release flag
    private static final int JAVA_VERSION_WITH_RELEASE_FLAG = 9;

    private final BuildConfig buildConfig;
    private final RuntimeConfiguration runtimeConfiguration;
    private final CompileTarget compileTarget;
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
                subdirName = "main";
                break;
            case TEST:
                subdirName = "test";
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }
    }
    
    private Path getBuildDir() {
        switch (compileTarget) {
            case MAIN:
                return Paths.get("loombuild", getModule().getPathName(), "classes", "main");
            case TEST:
                return Paths.get("loombuild", getModule().getPathName(), "classes", "test");
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
    public TaskResult run() throws Exception {
        final List<Path> classpath = new ArrayList<>();

        final Optional<SourceTreeProduct> sourceTreeProduct = getSourceTreeProduct();

        if (!sourceTreeProduct.isPresent()) {
            if (Files.exists(getBuildDir())) {
                FileUtil.deleteDirectoryRecursively(getBuildDir(), false);
            }

            return completeSkip();
        }

        final Path srcDir = sourceTreeProduct.get().getSrcDir();

        switch (compileTarget) {
            case MAIN:
                classpath.add(srcDir);
                useProduct("compileDependencies", ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            case TEST:
                classpath.add(srcDir);

                useProduct("compilation", CompilationProduct.class)
                    .map(CompilationProduct::getClassesDir)
                    .ifPresent(classpath::add);

                useProduct("testDependencies", ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);

                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }

        final List<Path> srcPaths = sourceTreeProduct.get().getSourceFiles();

//        final FileCacher fileCacher = runtimeConfiguration.isCacheEnabled()
//            ? new FileCacherImpl(cacheDir, subdirName) : new NullCacher();
//
//        if (fileCacher.filesCached(srcPaths)) {
//            return completeUpToDate(product());
//        }

        if (Files.notExists(getBuildDir())) {
            Files.createDirectories(getBuildDir());
        } else {
            FileUtil.deleteDirectoryRecursively(getBuildDir(), false);
        }

//        final List<File> srcFiles = srcPaths.stream()
//            .map(Path::toFile)
//            .collect(Collectors.toList());

        compile(classpath, srcPaths);

//        fileCacher.cacheFiles(srcPaths);

        return completeOk(product());
    }

    private Optional<SourceTreeProduct> getSourceTreeProduct() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("source", SourceTreeProduct.class);
            case TEST:
                return useProduct("testSource", SourceTreeProduct.class);
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    private CompilationProduct product() {
        switch (compileTarget) {
            case MAIN:
                return new CompilationProduct(getBuildDir());
            case TEST:
                return new CompilationProduct(getBuildDir());
            default:
                throw new IllegalStateException();
        }
    }

    private void compile(final List<Path> classpath, final List<Path> srcFiles) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diagnosticListener =
            new DiagnosticLogListener(LOG);

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH,
                new ArrayList<>(classpath));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(getBuildDir()));


            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));
            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));
            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));
            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));
            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));
            System.out.println("module path: '" + getModule().getModuleName() + "', path=" + Paths.get("modules", getModule().getPathName(), "src", subdirName, "java"));

            fileManager.setLocationForModule(StandardLocation.MODULE_SOURCE_PATH,
                getModule().getModuleName(),
                Collections.singletonList(Paths.get("modules", getModule().getPathName(), "src", subdirName, "java")));

            final List<File> files = srcFiles.stream()
                .map(Path::toFile)
                .collect(Collectors.toList());

            final Iterable<? extends JavaFileObject> compUnits =
                fileManager.getJavaFileObjectsFromFiles(files);

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
        options.add(getBuildDir().toString());

        options.add("-encoding");
        options.add("UTF-8");

        options.add("-Xlint:all");

        // http://blog.ltgt.net/most-build-tools-misuse-javac/

//        options.add("-sourcepath");
//        options.add("");

//        options.add("--module-source-path");
//        options.add("src/main/java");

        options.add("-Xpkginfo:always");

        options.addAll(
            configuredPlatformVersion(buildConfig.getBuildSettings().getJavaPlatformVersion()));

        return options;
    }

}
