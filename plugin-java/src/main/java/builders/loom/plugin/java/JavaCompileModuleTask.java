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
    private void compile(final List<Path> classpath, final List<Path> srcFiles)
        throws IOException, InterruptedException {

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diag = new DiagnosticLogListener(LOG);

        final Optional<JavaVersion> crossCompileVersion =
            configuredPlatformVersion(getModuleConfig().getBuildSettings().getJavaPlatformVersion())
            .map(JavaVersion::ofVersion)
            .filter(v -> !JavaVersion.current().equals(v));

        final Optional<Path> moduleInfoOpt = srcFiles.stream()
            .filter(f -> f.getFileName().toString().equals("module-info.java"))
            .findFirst();

        // Handle these cases:
        // Case 1 -- pure Java 9 compile with module-info.java --> MODULE_PATH
        // Case 2 -- pure Java 9 compile without module-info.java --> CLASS_PATH
        // Case 3 -- Cross-Compile without a module-info.java --> CLASS_PATH
        // Case 4 -- Cross-Compile with a module-info.java --> 1st run with MODULE_PATH,
        //           2nd run with CLASS_PATH, cross compile and without module-info.java

        if (moduleInfoOpt.isPresent()) {
            // Case 1 or 4

            if (crossCompileVersion.isPresent()) {
                // Case 4 - 1st step

                // Unfortunately JDK doesn't support cross-compile for module-info.java
                // First, compile everything with Java 9 (TODO what to do with Java 10?)

                LOG.debug("Compile {} java files (with module path)", srcFiles.size());
                compileSources(classpath, srcFiles, compiler, diag, true, JavaVersion.JAVA_9);

                // Case 4 - 2nd step
                // Then, compile everything but the module-info with requested Version
                final Path moduleInfo = moduleInfoOpt.get();
                final List<Path> srcFilesWithoutModuleInfo = srcFiles.stream()
                    .filter(f -> f != moduleInfo)
                    .collect(Collectors.toList());

                LOG.debug("Compile {} java files (Cross Compile for Java {})",
                    srcFilesWithoutModuleInfo.size(), crossCompileVersion.get());
                compileSources(classpath, srcFilesWithoutModuleInfo, compiler, diag, false,
                    crossCompileVersion.get());
            } else {
                // Case 1
                LOG.debug("Compile {} java files (with module path)", srcFiles.size());
                compileSources(classpath, srcFiles, compiler, diag, true, null);
            }
        } else {
            // Case 2 or 3
            if (crossCompileVersion.isPresent()) {
                // Case 3
                LOG.debug("Compile {} java files (Cross Compile for Java {})",
                    srcFiles.size(), crossCompileVersion.get());
                compileSources(classpath, srcFiles, compiler, diag, false,
                    crossCompileVersion.get());
            } else {
                // Case 2
                LOG.debug("Compile {} java files (with classpath)", srcFiles.size());
                compileSources(classpath, srcFiles, compiler, diag, false, null);
            }
        }
    }

    private StandardJavaFileManager newFileManager(
        final JavaCompiler compiler, final DiagnosticListener<JavaFileObject> diagnosticListener,
        final boolean useModulePath, final List<Path> classpath)
        throws IOException, InterruptedException {

        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8);

        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
            Collections.singletonList(getBuildDir()));

        if (useModulePath) {
            buildModulePath(classpath, fileManager);
        } else {
            buildClassPath(classpath, fileManager);
        }

        return fileManager;
    }

    private void compileSources(final List<Path> classpath, final List<Path> srcFiles,
                                final JavaCompiler compiler,
                                final DiagnosticListener<JavaFileObject> diag,
                                final boolean useModulePath, final JavaVersion release)
        throws IOException, InterruptedException {

        try (final StandardJavaFileManager fileManager =
                 newFileManager(compiler, diag, useModulePath, classpath)) {
            compile(compiler, diag, fileManager, buildOptions(release),
                fileManager.getJavaFileObjectsFromPaths(srcFiles));
        }
    }

    private void buildModulePath(final List<Path> classpath,
                                 final StandardJavaFileManager fileManager)
        throws InterruptedException, IOException {

        // Wait until other modules have delivered their compilations to module path
        for (final String moduleName : getModuleConfig().getModuleDependencies()) {
            // TODO doesn't work
/*
            final Optional<CompilationProduct> compilation =
                useProduct(moduleName, "compilation", CompilationProduct.class);
            if (compilation.isPresent()) {
                final Path moduleCompilePath = compilation.get().getClassesDir();
                LOG.debug("Modulepath for module {}: {}", moduleName, moduleCompilePath);
                fileManager.setLocationForModule(StandardLocation.MODULE_PATH,
                    moduleName, List.of(moduleCompilePath));
            }
*/

            // workaround - step 1/2
            getUsedProducts().getAndWaitProduct(moduleName, "compilation");
        }

        // workaround - step 2/2
        final List<Path> modulePath = new ArrayList<>();
        modulePath.add(getBuildDir().getParent());
        modulePath.addAll(classpath);
        fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, modulePath);
        LOG.debug("Modulepath: {}", modulePath);
    }

    private void buildClassPath(final List<Path> classpath,
                                final StandardJavaFileManager fileManager)
        throws InterruptedException, IOException {

        final List<Path> classPath = new ArrayList<>();

        // Wait until other modules have delivered their compilations to module path
        for (final String moduleName : getModuleConfig().getModuleDependencies()) {
            useProduct(moduleName, "compilation", CompilationProduct.class)
                .ifPresent(product -> classPath.add(product.getClassesDir()));
        }

        classPath.addAll(classpath);

        fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, classPath);
        LOG.debug("Classpath: {}", classPath);
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

    private static List<String> buildOptions(final JavaVersion release) {
        final List<String> options = new ArrayList<>();

        options.add("-Xlint:all");

        if (release != null) {
            options.add("--release");
            options.add(Integer.toString(release.getNumericVersion()));
        }

        return options;
    }

}
