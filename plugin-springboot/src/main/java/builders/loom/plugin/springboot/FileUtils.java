package builders.loom.plugin.springboot;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public final class FileUtils {

    private FileUtils() {
    }

    public static void copyFiles(final List<Path> sources, final Path dstDir)
        throws IOException {

        for (final Path source : sources) {
            Files.copy(source, dstDir.resolve(source.getFileName()));
        }
    }

    public static void copyFiles(final Path srcDir, final Path dstDir)
        throws IOException {

        Files.walkFileTree(srcDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(final Path dir,
                                                     final BasicFileAttributes attrs)
                throws IOException {
                Files.createDirectories(dstDir.resolve(srcDir.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path srcFile, final BasicFileAttributes srcAttr)
                throws IOException {

                Files.copy(srcFile, dstDir.resolve(srcDir.relativize(srcFile)));
                return FileVisitResult.CONTINUE;
            }

        });

    }

    public static void cleanDir(final Path rootPath) throws IOException {
        if (!Files.isDirectory(rootPath)) {
            return;
        }

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
    }

}
