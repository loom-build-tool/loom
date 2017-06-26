package jobt.plugin.java;

import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.ProvidedProducts;
import jobt.api.ResourcesTree;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.api.UsedProducts;


public class JavaProvideResourcesDirTask implements Task {
    private static final Path SRC_RES_PATH = Paths.get("src", "main", "resources");
    private static final Path SRC_TESTRES_PATH = Paths.get("src", "test", "resources");

    private final BuildConfig buildConfig;
    private final CompileTarget compileTarget;
    private final UsedProducts input;
    private final ProvidedProducts output;

    public JavaProvideResourcesDirTask(final BuildConfig buildConfig, final CompileTarget compileTarget, final UsedProducts input,
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
                output.complete("resources", new ResourcesTree(SRC_RES_PATH));
                return status;
            case TEST:
                output.complete("testResources", new ResourcesTree(SRC_TESTRES_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
