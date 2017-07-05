package builders.loom.api.product;

import java.nio.file.Path;

public final class ProcessedResourceProduct implements Product {

    private final Path resourcesDir;

    public ProcessedResourceProduct(final Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    public Path getSrcDir() {
        return resourcesDir;
    }

}
