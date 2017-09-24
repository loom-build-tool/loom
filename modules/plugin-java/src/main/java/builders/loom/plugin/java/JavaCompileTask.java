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
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class JavaCompileTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavaCompileTask.class);

    private final CompileTarget compileTarget;
    private final String sourceProductId;

    public JavaCompileTask(final CompileTarget compileTarget) {
        this.compileTarget = Objects.requireNonNull(compileTarget);

        switch (compileTarget) {
            case MAIN:
                sourceProductId = "source";
                break;
            case TEST:
                sourceProductId = "testSource";
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public TaskResult run(final boolean skip) throws Exception {
        final Path buildDir = resolveBuildDir();

        if (skip) {
            return TaskResult.up2date(new CompilationProduct(buildDir, ProductChecksumUtil.calcChecksum(buildDir)));
        }

        final Optional<Product> sourceTreeProduct =
            useProduct(sourceProductId, Product.class);

        if (!sourceTreeProduct.isPresent()) {
            FileUtil.deleteDirectoryRecursively(buildDir, true);
            return TaskResult.empty();
        }

        final List<Path> classpath = new ArrayList<>();

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

        final Path srcDir = Paths.get(sourceTreeProduct.get().getProperty("srcDir"));
        final List<Path> srcFiles = Files
            .find(srcDir, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())
            .collect(Collectors.toList());

        FileUtil.createOrCleanDirectory(buildDir);

        compile(buildDir, classpath, srcFiles);

        return TaskResult.ok(new CompilationProduct(buildDir, ProductChecksumUtil.calcChecksum(buildDir)));
    }

    private Path resolveBuildDir() {
        // TODO another workaround for non-functional MODULE_PATH
        return LoomPaths.buildDir(getRuntimeConfiguration().getProjectBaseDir())
            .resolve(Paths.get("compilation", compileTarget.name().toLowerCase(),
                getBuildContext().getModuleName()));
    }

    // read: http://blog.ltgt.net/most-build-tools-misuse-javac/

    private void compile(final Path buildDir, final List<Path> classpath,
                         final List<Path> srcFiles)
        throws IOException, InterruptedException {

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticListener<JavaFileObject> diag = new DiagnosticLogListener(LOG);

        final Optional<JavaVersion> crossCompileVersion =
            configuredPlatformVersion(getModuleConfig().getBuildSettings().getJavaPlatformVersion())
            .map(JavaVersion::ofVersion)
            .filter(v -> !JavaVersion.current().equals(v));

        final Optional<Path> moduleInfoOpt = srcFiles.stream()
            .filter(f -> f.getFileName().toString().equals(LoomPaths.MODULE_INFO_JAVA))
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
                // First, compile everything with current Java release

                LOG.debug("Compile {} java files (with module path)", srcFiles.size());
                compileSources(buildDir, classpath, srcFiles, compiler, diag,
                    true, null);

                // Case 4 - 2nd step
                // Then, compile everything but the module-info with requested Version
                final Path moduleInfo = moduleInfoOpt.get();
                final List<Path> srcFilesWithoutModuleInfo = srcFiles.stream()
                    .filter(f -> f != moduleInfo)
                    .collect(Collectors.toList());

                LOG.debug("Compile {} java files (Cross Compile for Java {})",
                    srcFilesWithoutModuleInfo.size(), crossCompileVersion.get());
                compileSources(buildDir, classpath, srcFilesWithoutModuleInfo, compiler, diag,
                    false, crossCompileVersion.get());
            } else {
                // Case 1
                LOG.debug("Compile {} java files (with module path)", srcFiles.size());
                compileSources(buildDir, classpath, srcFiles, compiler, diag, true, null);
            }
        } else {
            // Case 2 or 3
            if (crossCompileVersion.isPresent()) {
                // Case 3
                LOG.debug("Compile {} java files (Cross Compile for Java {})",
                    srcFiles.size(), crossCompileVersion.get());
                compileSources(buildDir, classpath, srcFiles, compiler, diag, false,
                    crossCompileVersion.get());
            } else {
                // Case 2
                LOG.debug("Compile {} java files (with classpath)", srcFiles.size());
                compileSources(buildDir, classpath, srcFiles, compiler, diag, false, null);
            }
        }
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

    private StandardJavaFileManager newFileManager(
        final Path buildDir, final JavaCompiler compiler,
        final DiagnosticListener<JavaFileObject> diagnosticListener,
        final boolean useModulePath, final List<Path> classpath)
        throws IOException, InterruptedException {

        final StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnosticListener, null, StandardCharsets.UTF_8);

        fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT,
            Collections.singletonList(buildDir));

        if (useModulePath) {
            buildModulePath(buildDir, classpath, fileManager);
        } else {
            buildClassPath(classpath, fileManager);
        }

        return fileManager;
    }

    private void buildModulePath(final Path buildDir, final List<Path> classpath,
                                 final StandardJavaFileManager fileManager)
        throws InterruptedException, IOException {

        // Wait until other modules have delivered their compilations to module path
        for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
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
        modulePath.add(buildDir.getParent());
        modulePath.addAll(classpath);
        fileManager.setLocationFromPaths(StandardLocation.MODULE_PATH, modulePath);
        LOG.debug("Modulepath: {}", modulePath);
    }

    private void buildClassPath(final List<Path> classpath,
                                final StandardJavaFileManager fileManager)
        throws InterruptedException, IOException {

        final List<Path> classPath = new ArrayList<>();

        // Wait until other modules have delivered their compilations to module path
        for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
            useProduct(moduleName, "compilation", CompilationProduct.class)
                .ifPresent(product -> classPath.add(product.getClassesDir()));
        }

        classPath.addAll(classpath);

        fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, classPath);
        LOG.debug("Classpath: {}", classPath);
    }

    private void compileSources(final Path buildDir, final List<Path> classpath,
                                final List<Path> srcFiles, final JavaCompiler compiler,
                                final DiagnosticListener<JavaFileObject> diag,
                                final boolean useModulePath, final JavaVersion release)
        throws IOException, InterruptedException {

        try (final StandardJavaFileManager fileManager =
                 newFileManager(buildDir, compiler, diag, useModulePath, classpath)) {
            compileSources(compiler, diag, fileManager, buildOptions(release),
                fileManager.getJavaFileObjectsFromPaths(srcFiles));
        }
    }

    private void compileSources(final JavaCompiler compiler,
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
