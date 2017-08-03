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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.JavaVersion;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.SourceTreeProduct;

public class JavaCompileModuleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileModuleTask.class);

    private final CompileTarget compileTarget;
    private final String subdirName;
    private final Path cacheDir;

    public JavaCompileModuleTask(final CompileTarget compileTarget,
                                 final Path cacheDir) {
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
        return LoomPaths.BUILD_DIR.resolve(Paths.get("compilation",
            compileTarget.name().toLowerCase(), getBuildContext().getModuleName()));
    }

    private static Optional<String> configuredPlatformVersion(final JavaVersion version) {
        Objects.requireNonNull(version, "versionString required");

        final int parsedJavaSpecVersion = JavaVersion.current().getNumericVersion();
        final int platformVersion = version.getNumericVersion();

        if (platformVersion == parsedJavaSpecVersion) {
            return Optional.empty();
        }

        return Optional.of(String.valueOf(platformVersion));
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

        switch (compileTarget) {
            case MAIN:
                useProduct("compileDependencies", ClasspathProduct.class)
                    .map(ClasspathProduct::getEntries)
                    .ifPresent(classpath::addAll);
                break;
            case TEST:
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

        final List<Path> srcFiles = sourceTreeProduct.get().getSourceFiles();

//        final FileCacher fileCacher = runtimeConfiguration.isCacheEnabled()
//            ? new FileCacherImpl(cacheDir, subdirName) : new NullCacher();
//
//        if (fileCacher.filesCached(srcFiles)) {
//            return completeUpToDate(product());
//        }

        if (Files.notExists(getBuildDir())) {
            Files.createDirectories(getBuildDir());
        } else {
            FileUtil.deleteDirectoryRecursively(getBuildDir(), false);
        }

//        final List<File> srcFiles = srcFiles.stream()
//            .map(Path::toFile)
//            .collect(Collectors.toList());


        // Wait until other modules have delivered their compilations to module path
        for (final String moduleName : getModuleConfig().getModuleDependencies()) {
            getUsedProducts().getAndWaitProduct(moduleName, "compilation");
        }

        compile(classpath, srcFiles);

//        fileCacher.cacheFiles(srcFiles);

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

    // read: http://blog.ltgt.net/most-build-tools-misuse-javac/
    private void compile(final List<Path> classpath, final List<Path> srcFiles) throws IOException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diagnosticListener =
            new DiagnosticLogListener(LOG);

        final Optional<String> javaVersion = configuredPlatformVersion(getModuleConfig()
            .getBuildSettings().getJavaPlatformVersion());

        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8)) {

            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
                Collections.singletonList(getBuildDir()));

            // TODO doesn't work
//            fileManager.setLocationForModule(StandardLocation.MODULE_PATH,
//                xxx, xxx);

            // workaround
            final List<Path> modulePath = buildModulePath(classpath);
            fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, modulePath);
            LOG.debug("Modulepath: {}", modulePath);

            if (javaVersion.isPresent()) {
                // Unfortunately JDK doesn't support cross-compile for module-info.java

                // First, compile module-info.java with current JDK version
                final Optional<Path> moduleInfoOpt = srcFiles.stream()
                    .filter(f -> f.getFileName().toString().equals("module-info.java"))
                    .findFirst();

                final List<Path> srcFilesWithoutModuleInfo;
                if (moduleInfoOpt.isPresent()) {
                    final Path moduleInfo = moduleInfoOpt.get();

                    LOG.debug("Compile {}", moduleInfo);
                    compile(compiler, diagnosticListener, fileManager, buildOptions(null),
                        fileManager.getJavaFileObjects(moduleInfo));

                    // Then, compile everything else with requested Version
                    srcFilesWithoutModuleInfo = srcFiles.stream()
                        .filter(f -> f != moduleInfo)
                        .collect(Collectors.toList());
                } else {
                    srcFilesWithoutModuleInfo = srcFiles;
                }

                LOG.debug("Compile {} java files", srcFilesWithoutModuleInfo.size());
                compile(compiler, diagnosticListener, fileManager, buildOptions(javaVersion.get()),
                    fileManager.getJavaFileObjectsFromPaths(srcFilesWithoutModuleInfo));
            } else {
                LOG.debug("Compile {} java files", srcFiles.size());
                compile(compiler, diagnosticListener, fileManager, buildOptions(null),
                    fileManager.getJavaFileObjectsFromPaths(srcFiles));
            }
        }
    }

    private void compile(final JavaCompiler compiler,
                         final DiagnosticListener<JavaFileObject> diagnosticListener,
                         final StandardJavaFileManager fileManager, final List<String> options,
                         final Iterable<? extends JavaFileObject> compUnits) {

        final JavaCompiler.CompilationTask compilerTask = compiler
            .getTask(null, fileManager, diagnosticListener, options, null, compUnits);

        if (!compilerTask.call()) {
            throw new IllegalStateException("Java compile failed");
        }
    }

    private List<Path> buildModulePath(final List<Path> classpath) {
        final List<Path> modulePath = new ArrayList<>();
        modulePath.add(getBuildDir().getParent());
        modulePath.addAll(classpath);
        return modulePath;
    }

    private static List<String> buildOptions(final String release) {
        final List<String> options = new ArrayList<>();

        options.add("-Xlint:all");

        if (release != null) {
            options.add("--release");
            options.add(release);
        }

        return options;
    }

}
