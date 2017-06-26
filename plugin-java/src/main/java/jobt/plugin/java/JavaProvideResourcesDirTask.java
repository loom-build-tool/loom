package jobt.plugin.java;

import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.CompileTarget;
import jobt.api.ResourcesTree;
import jobt.api.TaskStatus;


public class JavaProvideResourcesDirTask extends AbstractTask {
    private static final Path SRC_RES_PATH = Paths.get("src", "main", "resources");
    private static final Path SRC_TESTRES_PATH = Paths.get("src", "test", "resources");

    private final BuildConfig buildConfig;
    private final CompileTarget compileTarget;

    public JavaProvideResourcesDirTask(final BuildConfig buildConfig, final CompileTarget compileTarget) {
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
        switch(compileTarget) {
            case MAIN:
                getProvidedProducts().complete("resources", new ResourcesTree(SRC_RES_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("testResources", new ResourcesTree(SRC_TESTRES_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
