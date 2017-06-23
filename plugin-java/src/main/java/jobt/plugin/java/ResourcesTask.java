package jobt.plugin.java;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.ProcessedResource;
import jobt.api.CompileTarget;
import jobt.api.ProvidedProducts;
import jobt.api.RuntimeConfiguration;
import jobt.api.SourceTree;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.api.UsedProducts;

public class ResourcesTask implements Task {

    private final RuntimeConfiguration runtimeConfiguration;
    private final String subdirName;
    private final Path destPath;
    private final UsedProducts input;
    private final ProvidedProducts output;
    private final CompileTarget compileTarget;

    ResourcesTask(final RuntimeConfiguration runtimeConfiguration,
                  final CompileTarget compileTarget,
                  final UsedProducts input, final ProvidedProducts output) {

        this.runtimeConfiguration = runtimeConfiguration;
        this.compileTarget = compileTarget;
        this.input = input;
        this.output = output;

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

        destPath = Paths.get("jobtbuild", "resources", subdirName);
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {

        final SourceTree source = input.readProduct("source", SourceTree.class);
        final Path srcPath = source.getSrcDir();

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

        final Path cacheFile =
            Paths.get(".jobt", "cache", "java", "resource-" + subdirName + ".cache");

        final KeyValueCache cache = runtimeConfiguration.isCacheEnabled()
            ? new DiskKeyValueCache(cacheFile)
            : new NullKeyValueCache();

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath, cache));
        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath, cache));

        cache.saveCache();

        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch(compileTarget) {
            case MAIN:
                output.complete("processedResources", new ProcessedResource(destPath));
                return status;
            case TEST:
                output.complete("processedTestResources", new ProcessedResource(destPath));
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
