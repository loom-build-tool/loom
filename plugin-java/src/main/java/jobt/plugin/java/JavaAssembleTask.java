package jobt.plugin.java;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.TaskStatus;
import jobt.api.product.AssemblyProduct;
import jobt.api.product.CompilationProduct;
import jobt.api.product.ProcessedResourceProduct;
import jobt.api.product.ResourcesTreeProduct;
import jobt.api.product.SourceTreeProduct;

public class JavaAssembleTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final JavaPluginSettings pluginSettings;

    public JavaAssembleTask(final BuildConfig buildConfig,
                            final JavaPluginSettings pluginSettings) {
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskStatus run() throws Exception {
        final Manifest preparedManifest = prepareManifest();

        final Path buildDir = Files.createDirectories(Paths.get("jobtbuild", "libs"));

        // main jar
        final Path jarFile = buildDir.resolve(String.format("%s-%s.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));

        try (final JarOutputStream os = newJarOutputStream(jarFile, preparedManifest)) {
            final ProcessedResourceProduct resourcesTreeProduct = getUsedProducts().readProduct(
                "processedResources", ProcessedResourceProduct.class);
            copy(resourcesTreeProduct.getSrcDir(), os);

            final CompilationProduct compilation = getUsedProducts().readProduct(
                "compilation", CompilationProduct.class);
            copy(compilation.getClassesDir(), os);
        }

        // sources jar
        final Path sourceJarFile = buildDir.resolve(String.format("%s-%s-sources.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));

        try (final JarOutputStream os = newJarOutputStream(sourceJarFile, null)) {
            final ResourcesTreeProduct resourcesTreeProduct = getUsedProducts().readProduct(
                "resources", ResourcesTreeProduct.class);
            copy(resourcesTreeProduct.getSrcDir(), os);

            final SourceTreeProduct sourceTree = getUsedProducts().readProduct(
                "source", SourceTreeProduct.class);
            copy(sourceTree.getSrcDir(), os);
        }

        getProvidedProducts().complete("jar", new AssemblyProduct(jarFile));
        getProvidedProducts().complete("sourcesJar", new AssemblyProduct(sourceJarFile));

        return TaskStatus.OK;
    }

    private void copy(final Path srcDir, final JarOutputStream os) throws IOException {
        if (Files.isDirectory(srcDir)) {
            Files.walkFileTree(srcDir, new CreateJarFileVisitor(srcDir, os));
        }
    }

    private Manifest prepareManifest() {
        final Manifest newManifest = new Manifest();

        final ManifestBuilder manifestBuilder = new ManifestBuilder(newManifest);

        manifestBuilder
            .put(Attributes.Name.MANIFEST_VERSION, "1.0")
            .put("Created-By", "Jobt " + System.getProperty("jobt.version"))
            .put("Build-Jdk", String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        pluginSettings.getMainClassName()
            .ifPresent(s -> manifestBuilder.put(Attributes.Name.MAIN_CLASS, s));

        return newManifest;
    }

    private JarOutputStream newJarOutputStream(final Path targetFile, final Manifest manifest)
        throws IOException {
        return manifest == null
            ? new JarOutputStream(Files.newOutputStream(targetFile))
            : new JarOutputStream(Files.newOutputStream(targetFile), manifest);
    }

    private static class CreateJarFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceDir;
        private final JarOutputStream os;

        CreateJarFileVisitor(final Path sourceDir, final JarOutputStream os) {
            this.sourceDir = sourceDir;
            this.os = os;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                                                 final BasicFileAttributes attrs)
            throws IOException {

            if (dir.equals(sourceDir)) {
                return FileVisitResult.CONTINUE;
            }

            final JarEntry entry = new JarEntry(sourceDir.relativize(dir).toString() + "/");
            entry.setTime(attrs.lastModifiedTime().toMillis());
            os.putNextEntry(entry);
            os.closeEntry();

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs)
            throws IOException {

            final JarEntry entry = new JarEntry(sourceDir.relativize(file).toString());
            entry.setTime(attrs.lastModifiedTime().toMillis());
            os.putNextEntry(entry);
            Files.copy(file, os);
            os.closeEntry();
            return FileVisitResult.CONTINUE;
        }

    }
}
