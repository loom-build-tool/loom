package jobt.plugin.java;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class DeleteObsoleteFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetBasePath;
    private Path sourceBasePath;

    DeleteObsoleteFileVisitor(final Path targetBasePath) {
        this.targetBasePath = targetBasePath;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {

        if (sourceBasePath == null) {
            sourceBasePath = dir;
        } else if (Files.notExists(targetBasePath.resolve(sourceBasePath.relativize(dir)))) {
            FileUtil.deleteDirectoryRecursively(dir);
            return FileVisitResult.SKIP_SUBTREE;
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {

        if (Files.notExists(targetBasePath.resolve(sourceBasePath.relativize(file)))) {
            Files.delete(file);
        }

        return FileVisitResult.CONTINUE;
    }

}
