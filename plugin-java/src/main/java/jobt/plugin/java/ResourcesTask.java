package jobt.plugin.java;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import jobt.api.CompileTarget;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class ResourcesTask implements Task {

    private final Path srcPath;
    private final Path destPath;

    public ResourcesTask(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                srcPath = Paths.get("src", "main", "resources");
                destPath = Paths.get("jobtbuild", "resources", "main");
                break;
            case TEST:
                srcPath = Paths.get("src", "test", "resources");
                destPath = Paths.get("jobtbuild", "resources", "test");
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        if (!Files.isDirectory(srcPath)) {
            return TaskStatus.SKIP;
        }

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath));

        return TaskStatus.OK;
    }

    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {

        private final Path targetPath;
        private Path sourcePath;

        CopyFileVisitor(final Path targetPath) {
            this.targetPath = targetPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
            throws IOException {

            if (sourcePath == null) {
                sourcePath = dir;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
            throws IOException {

            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }

}
