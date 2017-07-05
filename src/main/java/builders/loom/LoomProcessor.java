package builders.loom;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.plugin.TaskRegistryImpl;
import builders.loom.plugin.TaskUtil;
import builders.loom.api.ProductRepository;
import builders.loom.config.BuildConfigWithSettings;
import builders.loom.plugin.PluginRegistry;
import builders.loom.plugin.ProductRepositoryImpl;
import builders.loom.plugin.ServiceLocatorImpl;
import builders.loom.plugin.TaskRegistryLookup;
import builders.loom.util.Stopwatch;

public class LoomProcessor {

    private final TaskRegistryLookup taskRegistry = new TaskRegistryImpl();
    private final ServiceLocatorImpl serviceLocator = new ServiceLocatorImpl();
    private final ProductRepository productRepository = new ProductRepositoryImpl();

    static {
        System.setProperty("loom.version", Version.getVersion());
    }

    public void init(final BuildConfigWithSettings buildConfig,
                     final RuntimeConfigurationImpl runtimeConfiguration) {
        Stopwatch.startProcess("Initialize plugins");
        new PluginRegistry(buildConfig, runtimeConfiguration,
            taskRegistry, serviceLocator).initPlugins();
        Stopwatch.stopProcess();
    }

    public void execute(final List<String> productIds) throws Exception {
        final TaskRunner taskRunner = new TaskRunner(
            taskRegistry, productRepository, serviceLocator);
        taskRunner.execute(new HashSet<>(productIds));
    }

    public void clean() {
        cleanDir(Paths.get("loombuild"));
        cleanDir(Paths.get(".loom"));
    }

    private static void cleanDir(final Path rootPath) {
        if (!Files.isDirectory(rootPath)) {
            return;
        }

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                    throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void logMemoryUsage() {
        final Logger log = LoggerFactory.getLogger(LoomProcessor.class);

        final Runtime rt = Runtime.getRuntime();
        final long maxMemory = rt.maxMemory();
        final long totalMemory = rt.totalMemory();
        final long freeMemory = rt.freeMemory();
        final long memUsed = totalMemory - freeMemory;

        log.debug("Memory max={}, total={}, free={}, used={}",
            maxMemory, totalMemory, freeMemory, memUsed);
    }

    public void generateDotProductOverview() {
        GraphvizOutput.generateDot(taskRegistry);
    }

    public Collection<String> getAvailableProducts() {
        return
            TaskUtil.buildInvertedProducersMap(taskRegistry).keySet();
    }

}
