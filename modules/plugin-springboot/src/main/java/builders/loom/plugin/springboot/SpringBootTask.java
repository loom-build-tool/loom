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

package builders.loom.plugin.springboot;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.DirectoryProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.util.FileUtil;
import builders.loom.util.Iterables;
import builders.loom.util.Preconditions;

public class SpringBootTask extends AbstractModuleTask {

    private static final String SPRING_BOOT_APPLICATION_ANNOTATION =
        "org.springframework.boot.autoconfigure.SpringBootApplication";

    private static final String SPRING_BOOT_LAUNCHER =
        "org.springframework.boot.loader.JarLauncher";

    private final SpringBootPluginSettings pluginSettings;

    public SpringBootTask(final SpringBootPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskResult run() throws Exception {
        final Path buildDir = FileUtil.createOrCleanDirectory(resolveBuildDir("springboot"));

        final Path metaInfDir = Files.createDirectories(
            buildDir.resolve("META-INF"));

        final Path classesDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "classes")));

        final Path libDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "lib")));

        // copy resources
        final Optional<ProcessedResourceProduct> resourcesTreeProduct =
            useProduct("processedResources", ProcessedResourceProduct.class);
        resourcesTreeProduct.ifPresent(processedResourceProduct ->
            FileUtil.copyFiles(processedResourceProduct.getSrcDir(), classesDir));

        // copy classes
        final CompilationProduct compilationProduct =
            requireProduct("compilation", CompilationProduct.class);
        FileUtil.copyFiles(compilationProduct.getClassesDir(), classesDir);

        // copy libs
        final ClasspathProduct compileDependenciesProduct =
            requireProduct("compileDependencies", ClasspathProduct.class);
        FileUtil.copyFiles(compileDependenciesProduct.getEntries(), libDir);

        // copy dep modules
        for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
            final Path jarFile =
                requireProduct(moduleName, "jar", AssemblyProduct.class).getAssemblyFile();

            Files.copy(jarFile, libDir.resolve(jarFile.getFileName()));
        }

        // copy spring boot loader
        copySpringBootLoader(resolveSpringBootLoaderJar(), buildDir);

        // create META-INF/MANIFEST.MF
        final String applicationStarter = scanForApplicationStarter(compilationProduct);
        writeManifest(buildDir, prepareManifest(applicationStarter));

        return completeOk(new DirectoryProduct(buildDir, "Spring Boot application"));
    }

    private Path resolveSpringBootLoaderJar() {
        final DependencyResolverService mavenDependencyResolver = getServiceLocator()
            .getService("mavenDependencyResolver", DependencyResolverService.class);

        final String springBootLoaderArtifact =
            "org.springframework.boot:spring-boot-loader:" + pluginSettings.getVersion();

        final List<Path> resolvedArtifacts = mavenDependencyResolver.resolve(
            Collections.singletonList(springBootLoaderArtifact),
            DependencyScope.COMPILE, "springBootApplication");

        return Iterables.getOnlyElement(resolvedArtifacts);
    }

    private void copySpringBootLoader(final Path springBootLoaderJar, final Path baseDir)
        throws IOException {

        Preconditions.checkState(Files.isRegularFile(springBootLoaderJar));

        try (final JarFile jarFile = new JarFile(springBootLoaderJar.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().startsWith("META-INF/")) {
                    continue;
                }
                if (jarEntry.isDirectory()) {
                    Files.createDirectories(baseDir.resolve(jarEntry.getName()));
                } else {
                    try (final InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        Files.copy(inputStream, baseDir.resolve(jarEntry.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private String scanForApplicationStarter(final CompilationProduct compilationProduct)
        throws IOException {

        final String applicationClassname = new ClassScanner()
            .scanArchives(compilationProduct.getClassesDir(), SPRING_BOOT_APPLICATION_ANNOTATION);

        if (applicationClassname == null) {
            throw new IllegalStateException("Couldn't find class with "
                + SPRING_BOOT_APPLICATION_ANNOTATION + " annotation");
        }

        return applicationClassname;
    }

    private Manifest prepareManifest(final String applicationClassname) {
        final Manifest manifest = new Manifest();

        new ManifestBuilder(manifest)
            .put(Attributes.Name.MANIFEST_VERSION, "1.0")
            .put("Created-By", "Loom " + System.getProperty("loom.version"))
            .put("Build-Jdk", String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")))
            .put("Start-Class", applicationClassname)
            .put("Spring-Boot-Classes", "BOOT-INF/classes/")
            .put("Spring-Boot-Lib", "BOOT-INF/lib/")
            .put("Spring-Boot-Version", pluginSettings.getVersion())
            .put(Attributes.Name.MAIN_CLASS, SPRING_BOOT_LAUNCHER);

        return manifest;
    }

    private void writeManifest(final Path buildDir, final Manifest manifest) throws IOException {
        final Path manifestDir = Files.createDirectories(buildDir.resolve("META-INF"));
        final Path file = manifestDir.resolve("MANIFEST.MF");
        try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
            manifest.write(os);
        }
    }

}
