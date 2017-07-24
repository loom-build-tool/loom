package builders.loom.api;

import java.nio.file.Path;

public class Module implements BuildContext {

    private final String moduleName;
    private final Path path;
    private final ModuleBuildConfig config;

    public Module(final String moduleName, final Path path, final ModuleBuildConfig config) {
        this.moduleName = moduleName;
        this.path = path;
        this.config = config;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public ModuleBuildConfig getConfig() {
        return config;
    }

}
