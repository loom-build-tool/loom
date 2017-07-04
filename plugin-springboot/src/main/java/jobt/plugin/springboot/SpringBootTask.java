package jobt.plugin.springboot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.DependencyResolverService;
import jobt.api.DependencyScope;
import jobt.api.TaskStatus;
import jobt.api.product.AssemblyProduct;
import jobt.api.product.ClasspathProduct;
import jobt.api.product.CompilationProduct;
import jobt.api.product.ProcessedResourceProduct;
import jobt.util.Iterables;
import jobt.util.Preconditions;

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
        FileUtils.cleanDir(baseDir);

        final Path buildDir = baseDir.resolve("build");

        final Path jarFile = baseDir.resolve(String.format("%s-%s.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));

        final Path assemblyFile = baseDir.resolve(jarFile.getFileName());

        // copy resources
        final ProcessedResourceProduct resourcesTreeProduct = getUsedProducts()
            .readProduct("processedResources", ProcessedResourceProduct.class);
        final Path classesDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "classes")));
        FileUtils.copyFiles(resourcesTreeProduct.getSrcDir(), classesDir);

        // copy classes
        final CompilationProduct compilationProduct = getUsedProducts()
            .readProduct("compilation", CompilationProduct.class);
        FileUtils.copyFiles(compilationProduct.getClassesDir(), classesDir);

        // scan for @SpringBootApplication
        final String applicationClassname = scanForApplicationStarter(compilationProduct);

        // copy libs
        final ClasspathProduct compileDependenciesProduct = getUsedProducts()
            .readProduct("compileDependencies", ClasspathProduct.class);
        final Path libDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "lib")));
        FileUtils.copyFiles(compileDependenciesProduct.getEntries(), libDir);

        // copy spring boot loader
        copySpringBootLoader(resolveSpringBootLoaderJar(), buildDir);

        // assemble jar
        new JarAssembler(pluginSettings).assemble(buildDir, assemblyFile, applicationClassname);

        return complete(TaskStatus.OK, assemblyFile);
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

    private TaskStatus complete(final TaskStatus status, final Path assemblyFile) {
        getProvidedProducts().complete("springBootApplication", new AssemblyProduct(assemblyFile));
        return status;
    }

}
