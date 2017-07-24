package builders.loom.api;

import java.nio.file.Path;

public class GlobalBuildContext implements BuildContext {

    @Override
    public String getModuleName() {
        return "global";
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
