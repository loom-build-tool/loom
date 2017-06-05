package jobt.plugin.java;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public final class FileUtil {

    private FileUtil() {
    }

    public static void deleteDirectoryRecursively(final Path dir) {
        checkDir(dir);
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
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

    public static void syncDir(final Path srcPath, final Path destPath) throws IOException {
        checkDir(srcPath);

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath));
        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath));
    }

    private static void checkDir(final Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory '" + dir + " doesn't exist");
        }
    }

}
