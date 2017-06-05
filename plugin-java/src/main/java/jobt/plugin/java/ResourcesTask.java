package jobt.plugin.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.CompileTarget;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class ResourcesTask implements Task {

    private final Path srcPath;
    private final Path destPath;

    ResourcesTask(final CompileTarget compileTarget) {
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
        if (Files.notExists(srcPath) && Files.notExists(destPath)) {
            return TaskStatus.SKIP;
        }

        assertDirectory(srcPath);
        assertDirectory(destPath);

        if (Files.notExists(srcPath) && Files.exists(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath);
        } else {
            FileUtil.syncDir(srcPath, destPath);
        }

        return TaskStatus.OK;
    }

    private void assertDirectory(final Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Path '" + path + "' is not a directory");
        }
    }

}
