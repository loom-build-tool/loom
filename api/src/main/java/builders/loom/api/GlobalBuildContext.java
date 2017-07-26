package builders.loom.api;

import java.nio.file.Path;

public class GlobalBuildContext implements BuildContext {

    public static final String GLOBAL_MODULE_NAME = "global";

    @Override
    public String getModuleName() {
        return GLOBAL_MODULE_NAME;
    }

    @Override
    public Path getPath() {
        return LoomPaths.PROJECT_DIR;
    }

    @Override
    public BuildConfig getConfig() {
        return new GlobalBuildConfig();
    }

}
