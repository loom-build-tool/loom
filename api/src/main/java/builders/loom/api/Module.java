package builders.loom.api;

import java.nio.file.Path;

public class Module {

    private final String pathName;
    private final String moduleName;
    private final Path path;
    private final BuildConfigWithSettings config;
    private final boolean globalModule;

    public Module(final String pathName, final String moduleName, final Path path, final BuildConfigWithSettings config, boolean globalModule) {
        this.pathName = pathName;
        this.moduleName = moduleName;
        this.path = path;
        this.config = config;
        this.globalModule = globalModule;
    }

    /**
     * Typically same as module name, but may differ (e.g. modulename=com.example.api; path=api).
     */
    public String getPathName() {
        return pathName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getPath() {
        return path;
    }

    public BuildConfigWithSettings getConfig() {
        return config;
    }

    public boolean isGlobalModule() {
        return globalModule;
    }

}
