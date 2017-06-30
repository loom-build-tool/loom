package jobt.plugin.java;

import java.nio.file.Path;
import java.nio.file.Paths;

import jobt.api.AbstractTask;
import jobt.api.CompileTarget;
import jobt.api.TaskStatus;
import jobt.api.product.ResourcesTreeProduct;

public class JavaProvideResourcesDirTask extends AbstractTask {
    private static final Path SRC_RES_PATH = Paths.get("src", "main", "resources");
    private static final Path SRC_TESTRES_PATH = Paths.get("src", "test", "resources");

    private final CompileTarget compileTarget;

    public JavaProvideResourcesDirTask(final CompileTarget compileTarget) {
        this.compileTarget = compileTarget;
    }

    @Override
    public TaskStatus run() throws Exception {
        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("resources",
                    new ResourcesTreeProduct(SRC_RES_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("testResources",
                    new ResourcesTreeProduct(SRC_TESTRES_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
