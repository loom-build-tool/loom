package jobt.plugin.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.CompileTarget;
import jobt.api.RuntimeConfiguration;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class ResourcesTask implements Task {

    private final RuntimeConfiguration runtimeConfiguration;
    private final String subdirName;
    private final Path srcPath;
    private final Path destPath;

    ResourcesTask(final RuntimeConfiguration runtimeConfiguration,
                  final CompileTarget compileTarget) {

        this.runtimeConfiguration = runtimeConfiguration;

        switch (compileTarget) {
            case MAIN:
                subdirName = "main";
                break;
            case TEST:
                subdirName = "test";
                break;
            default:
                throw new IllegalArgumentException("Unknown compileTarget " + compileTarget);
        }

        srcPath = Paths.get("src", subdirName, "resources");
        destPath = Paths.get("jobtbuild", "resources", subdirName);
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        if (Files.notExists(srcPath) && Files.notExists(destPath)) {
            return TaskStatus.SKIP;
        }

        assertDirectoryOrMissing(srcPath);
        assertDirectoryOrMissing(destPath);

        if (Files.notExists(srcPath) && Files.exists(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath, true);
            return TaskStatus.OK;
        }

        FileUtil.assertDirectory(srcPath);

        final Path cacheFile =
            Paths.get(".jobt", "cache", "java", "resource-" + subdirName + ".cache");

        final KeyValueCache cache = runtimeConfiguration.isCacheEnabled()
            ? new DiskKeyValueCache(cacheFile)
            : new NullKeyValueCache();

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath, cache));
        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath, cache));

        cache.saveCache();

        return TaskStatus.OK;
    }

    private void assertDirectoryOrMissing(final Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Path '" + path + "' is not a directory");
        }
    }

}
