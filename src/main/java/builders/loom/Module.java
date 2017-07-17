package builders.loom;

import java.nio.file.Path;

import builders.loom.api.BuildConfig;

public class Module {

    private final String name;
    private final Path path;
    private final BuildConfig config;

    public Module(final String name, final Path path, final BuildConfig config) {
        this.name = name;
        this.path = path;
        this.config = config;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public BuildConfig getConfig() {
        return config;
    }

}
