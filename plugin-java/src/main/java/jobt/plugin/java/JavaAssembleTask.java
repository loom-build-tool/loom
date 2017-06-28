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
import jobt.api.AssemblyProduct;
import jobt.api.BuildConfig;
import jobt.api.CompilationProduct;
import jobt.api.SourceTreeProduct;
import jobt.api.TaskStatus;

public class JavaAssembleTask extends AbstractTask {

    private final BuildConfig buildConfig;

    public JavaAssembleTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public TaskStatus run() throws Exception {
        final Manifest preparedManifest = prepareManifest();

        final Path buildDir = Paths.get("jobtbuild", "libs");
        Files.createDirectories(buildDir);

        final Path jarFile = buildDir.resolve(String.format("%s-%s.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));
        final CompilationProduct compilation = getUsedProducts().readProduct(
            "compilation", CompilationProduct.class);
        createJar(compilation.getClassesDir(), jarFile, preparedManifest);

        final Path sourceJarFile = buildDir.resolve(String.format("%s-%s-sources.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));
        final SourceTreeProduct sourceTree = getUsedProducts().readProduct(
            "source", SourceTreeProduct.class);
        createJar(sourceTree.getSrcDir(), sourceJarFile, null);

        getProvidedProducts().complete("jar", new AssemblyProduct(jarFile));
        getProvidedProducts().complete("sourcesJar", new AssemblyProduct(sourceJarFile));

        return TaskStatus.OK;
    }

    private void createJar(final Path sourceDir, final Path targetFile, final Manifest manifest)
        throws IOException {
        try (final JarOutputStream os = newJarOutputStream(targetFile, manifest)) {
            Files.walkFileTree(sourceDir, new CreateJarFileVisitor(sourceDir, os));
        }
    }

    private Manifest prepareManifest() {
        final Manifest newManifest = new Manifest();
        final Attributes mainAttributes = newManifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"),
            "Jobt " + System.getProperty("jobt.version"));
        mainAttributes.put(new Attributes.Name("Build-Jdk"),
            String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        final String mainClassName = buildConfig.getConfiguration().get("mainClassName");
        if (mainClassName != null) {
            mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClassName);
        }
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
