package jobt.plugin.springboot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.TaskStatus;
import jobt.api.product.AssemblyProduct;
import jobt.api.product.ClasspathProduct;
import jobt.api.product.CompilationProduct;
import jobt.api.product.ProcessedResourceProduct;

public class SpringBootTask extends AbstractTask {

    private static final String SPRING_BOOT_APPLICATION_ANNOTATION =
        "org.springframework.boot.autoconfigure.SpringBootApplication";

    private final BuildConfig buildConfig;
    private final SpringBootPluginSettings pluginSettings;

    public SpringBootTask(final BuildConfig buildConfig,
                          final SpringBootPluginSettings pluginSettings) {
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskStatus run() throws Exception {
        final Path baseDir = Paths.get("jobtbuild", "springboot");
        final Path buildDir = baseDir.resolve("build");

        final Path jarFile = baseDir.resolve(String.format("%s-%s.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));

        final Path assemblyFile = baseDir.resolve(jarFile.getFileName());

        // copy classes
        final CompilationProduct compilationProduct = getUsedProducts()
            .readProduct("compilation", CompilationProduct.class);
        final Path classesDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "classes")));
        copyFiles(compilationProduct.getClassesDir(), classesDir);

        // copy resources
        final ProcessedResourceProduct resourcesTreeProduct = getUsedProducts()
            .readProduct("processedResources", ProcessedResourceProduct.class);
        copyFiles(resourcesTreeProduct.getSrcDir(), classesDir);

        // scan for @SpringBootApplication
        final String applicationClassname = scanForApplicationStarter(compilationProduct);

        // copy libs
        final ClasspathProduct compileDependenciesProduct = getUsedProducts()
            .readProduct("compileDependencies", ClasspathProduct.class);
        final Path libDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "lib")));
        copyLibs(libDir, compileDependenciesProduct);

        // copy spring boot loader
        final ClasspathProduct pluginDependenciesProduct = getUsedProducts()
            .readProduct("pluginDependencies.springBootApplication", ClasspathProduct.class);
        copySpringBootLoader(buildDir, pluginDependenciesProduct);

        // assemble jar
        new JarAssembler(pluginSettings).assemble(buildDir, assemblyFile, applicationClassname);

        return complete(TaskStatus.OK, assemblyFile);
    }

    private void copyFiles(final Path srcDir, final Path targetDir)
        throws IOException {

        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                                                     final BasicFileAttributes attrs)
                throws IOException {
                Files.createDirectories(targetDir.resolve(srcDir.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                throws IOException {
                Files.copy(file, targetDir.resolve(srcDir.relativize(file)));
                return FileVisitResult.CONTINUE;
            }

        });
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

    private void copyLibs(final Path libDir, final ClasspathProduct compileDependencies)
        throws IOException {

        for (final Path path : compileDependencies.getEntries()) {
            Files.copy(path, libDir.resolve(path.getFileName()));
        }
    }

    private void copySpringBootLoader(final Path baseDir,
                                      final ClasspathProduct pluginDependenciesProduct)
        throws IOException {

        final Path springBootLoaderJar = pluginDependenciesProduct.getSingleEntry();

        try (final JarFile jarFile = new JarFile(springBootLoaderJar.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                if ("META-INF/MANIFEST.MF".equals(jarEntry.getName())) {
                    continue;
                }
                if (jarEntry.isDirectory()) {
                    Files.createDirectories(baseDir.resolve(jarEntry.getName()));
                } else {
                    try (final InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        Files.copy(inputStream, baseDir.resolve(jarEntry.getName()));
                    }
                }
            }
        }
    }

    private TaskStatus complete(final TaskStatus status, final Path assemblyFile) {
        getProvidedProducts().complete("springBootApplication", new AssemblyProduct(assemblyFile));
        return status;
    }

}
