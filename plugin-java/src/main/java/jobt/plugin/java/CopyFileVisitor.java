package jobt.plugin.java;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetBasePath;
    private Path sourceBasePath;

    CopyFileVisitor(final Path targetBasePath) throws IOException {
        this.targetBasePath = targetBasePath;
        Files.createDirectories(targetBasePath);
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {

        if (sourceBasePath == null) {
            sourceBasePath = dir;
        } else {
            final Path destPath = targetBasePath.resolve(sourceBasePath.relativize(dir));
            if (Files.exists(destPath) && !Files.isDirectory(destPath)) {
                Files.delete(destPath);
            }

            if (Files.notExists(destPath)) {
                Files.createDirectory(destPath);
            }
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {

        final Path destPath = targetBasePath.resolve(sourceBasePath.relativize(file));

        if (Files.exists(destPath) && Files.isDirectory(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath);
        }

        Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }

}
