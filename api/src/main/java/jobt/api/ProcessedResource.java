package jobt.api;

import java.nio.file.Path;

public final class ProcessedResource implements Product {

    private final Path resourcesDir;

    public ProcessedResource(final Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public Path getSrcDir() {
        return resourcesDir;
    }

}
