package jobt.plugin.java;

import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.ProvidedProducts;
import jobt.api.SourceTree;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.api.UsedProducts;


public class JavaProvideSourceDirTask implements Task {

    private static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    private static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");

    private final BuildConfig buildConfig;
    private final CompileTarget compileTarget;
    private final UsedProducts input;
    private final ProvidedProducts output;

    public JavaProvideSourceDirTask(final BuildConfig buildConfig, final CompileTarget compileTarget, final UsedProducts input,
        final ProvidedProducts output) {
            this.buildConfig = buildConfig;
            this.compileTarget = compileTarget;
            this.input = input;
            this.output = output;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch(compileTarget) {
            case MAIN:
                output.complete("source", new SourceTree(SRC_MAIN_PATH));
                return status;
            case TEST:
                output.complete("testSource", new SourceTree(SRC_TEST_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
