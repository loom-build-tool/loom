package jobt.plugin.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.AbstractTask;
import jobt.api.CompileTarget;
import jobt.api.ProcessedResource;
import jobt.api.ResourcesTree;
import jobt.api.RuntimeConfiguration;
import jobt.api.TaskStatus;

public class ResourcesTask extends AbstractTask {

    private final RuntimeConfiguration runtimeConfiguration;
    private final Path destPath;
    private final CompileTarget compileTarget;

    ResourcesTask(final RuntimeConfiguration runtimeConfiguration,
                  final CompileTarget compileTarget) {

        this.runtimeConfiguration = runtimeConfiguration;
        this.compileTarget = compileTarget;

        destPath = Paths.get("jobtbuild", "resources", compileTarget.name().toLowerCase());
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {

        final Path srcPath;

        switch (compileTarget) {
            case MAIN:
                srcPath = getUsedProducts().readProduct("resources", ResourcesTree.class).getSrcDir();
                break;
            case TEST:
                srcPath = getUsedProducts().readProduct("testResources", ResourcesTree.class).getSrcDir();
                break;
            default:
                throw new IllegalStateException();
        }

        if (Files.notExists(srcPath) && Files.notExists(destPath)) {
            return complete(TaskStatus.SKIP);
        }

        assertDirectoryOrMissing(srcPath);
        assertDirectoryOrMissing(destPath);

        if (Files.notExists(srcPath) && Files.exists(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath, true);
            return complete(TaskStatus.OK);
        }

        FileUtil.assertDirectory(srcPath);

        final Path cacheFile = getCacheFileName();

        final KeyValueCache cache = runtimeConfiguration.isCacheEnabled()
            ? new DiskKeyValueCache(cacheFile)
            : new NullKeyValueCache();

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath, cache));
        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath, cache));

        cache.saveCache();

        return complete(TaskStatus.OK);
    }

    private Path getCacheFileName() {
        return Paths.get(".jobt", "cache", "java", "resource-" + compileTarget.name().toLowerCase() + ".cache");
    }

    private TaskStatus complete(final TaskStatus status) {
        switch(compileTarget) {
            case MAIN:
                getProvidedProducts().complete("processedResources", new ProcessedResource(destPath));
                return status;
            case TEST:
                getProvidedProducts().complete("processedTestResources", new ProcessedResource(destPath));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private void assertDirectoryOrMissing(final Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Path '" + path + "' is not a directory");
        }
    }

}
