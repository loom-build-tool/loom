package jobt.plugin.java;

import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.SourceTreeProduct;
import jobt.api.TaskStatus;

public class JavaProvideSourceDirTask extends AbstractTask {

    private static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    private static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");

    private final BuildConfig buildConfig;
    private final CompileTarget compileTarget;

    public JavaProvideSourceDirTask(
        final BuildConfig buildConfig, final CompileTarget compileTarget) {
        this.buildConfig = buildConfig;
        this.compileTarget = compileTarget;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("source", new SourceTreeProduct(SRC_MAIN_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("testSource", new SourceTreeProduct(SRC_TEST_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
