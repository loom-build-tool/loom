package jobt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.config.BuildConfigImpl;
import jobt.util.Stopwatch;

public class JobtProcessor {

    private TaskTemplateImpl taskTemplate;

    static {
        System.setProperty("jobt.version", Version.getVersion());
    }

    public void init(final BuildConfigImpl buildConfig,
                     final RuntimeConfigurationImpl runtimeConfiguration) {
        Stopwatch.startProcess("Initialize plugins");
        taskTemplate = new TaskTemplateImpl(buildConfig, runtimeConfiguration);
        Stopwatch.stopProcess();
    }

    public void execute(final String[] taskNames) throws Exception {
        taskTemplate.execute(taskNames);
    }

    public void clean() throws ExecutionException, InterruptedException {
        cleanDir(Paths.get("jobtbuild"));
        cleanDir(Paths.get(".jobt"));
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
        final Logger log = LoggerFactory.getLogger(JobtProcessor.class);

        final Runtime rt = Runtime.getRuntime();
        final long maxMemory = rt.maxMemory();
        final long totalMemory = rt.totalMemory();
        final long freeMemory = rt.freeMemory();
        final long memUsed = totalMemory - freeMemory;

        log.debug("Memory max={}, total={}, free={}, used={}",
            maxMemory, totalMemory, freeMemory, memUsed);
    }

}
