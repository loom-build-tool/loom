package builders.loom.plugin.java;

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

    public static void deleteDirectoryRecursively(final Path parent, final boolean parentItself) {
        assertDirectory(parent);
        try {
            Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                    throws IOException {
                    if (parentItself || !dir.equals(parent)) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void assertDirectory(final Path dir) {
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Directory '" + dir + " doesn't exist");
        }
    }

}
