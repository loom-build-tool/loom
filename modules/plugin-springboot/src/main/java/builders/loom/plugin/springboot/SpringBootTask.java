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
import java.util.stream.Collectors;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskResult;
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.Iterables;
import builders.loom.util.Preconditions;
import builders.loom.util.ProductChecksumUtil;

public class SpringBootTask extends AbstractModuleTask {

    private static final String SPRING_BOOT_APPLICATION_ANNOTATION =
        "org.springframework.boot.autoconfigure.SpringBootApplication";

    private static final String SPRING_BOOT_LAUNCHER =
        "org.springframework.boot.loader.JarLauncher";

    private final SpringBootPluginSettings pluginSettings;
    private final DependencyResolverService dependencyResolverService;

    public SpringBootTask(final SpringBootPluginSettings pluginSettings,
                          final DependencyResolverService dependencyResolverService) {
        this.pluginSettings = pluginSettings;
        this.dependencyResolverService = dependencyResolverService;
    }

    @Override
    public TaskResult run() throws Exception {
        final Path buildDir = FileUtil.createOrCleanDirectory(resolveBuildDir("springboot"));

        final Path classesDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "classes")));

        final Path libDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "lib")));

        // copy resources
        final Optional<Product> resourcesTreeProduct =
            useProduct("processedResources", Product.class);
        resourcesTreeProduct.ifPresent(processedResourceProduct -> FileUtil.copyFiles(
            Paths.get(processedResourceProduct.getProperty("processedResourcesDir")), classesDir));

        // copy classes
        final Product compilationProduct =
            requireProduct("compilation", Product.class);
        FileUtil.copyFiles(Paths.get(compilationProduct.getProperty("classesDir")), classesDir);

        // copy libs
        final List<Path> compileDependencies =
            requireProduct("compileDependencies", Product.class)
            .getProperties("classpath").stream()
            .map(p -> Paths.get(p))
            .collect(Collectors.toList());
        FileUtil.copyFiles(compileDependencies, libDir);

        // copy dep modules
        for (final String moduleName : getModuleConfig().getModuleCompileDependencies()) {
            final Path jarFile = Paths.get(requireProduct(moduleName, "jar", Product.class)
                .getProperty("classesJarFile"));

            Files.copy(jarFile, libDir.resolve(jarFile.getFileName()));
        }

        // copy spring boot loader
        copySpringBootLoader(resolveSpringBootLoaderJar(), buildDir);

        // create META-INF/MANIFEST.MF
        final String applicationStarter = scanForApplicationStarter(compilationProduct);
        writeManifest(buildDir, prepareManifest(applicationStarter));

        return TaskResult.ok(newProduct(buildDir));
    }

    private Path resolveSpringBootLoaderJar() {
        final String springBootLoaderArtifact =
            "org.springframework.boot:spring-boot-loader:" + pluginSettings.getVersion();

        final List<Path> resolvedArtifacts = dependencyResolverService.resolveMainArtifacts(
            Collections.singletonList(springBootLoaderArtifact),
            DependencyScope.COMPILE);

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

    private String scanForApplicationStarter(final Product compilationProduct)
        throws IOException {

        final String applicationClassname = new ClassScanner()
            .scanArchives(Paths.get(compilationProduct.getProperty("classesDir")),
                SPRING_BOOT_APPLICATION_ANNOTATION);

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

    private static Product newProduct(final Path buildDir) {
        return new GenericProduct("springBootOut", buildDir.toString(),
            ProductChecksumUtil.calcChecksum(buildDir), "Spring Boot application");
    }

}
