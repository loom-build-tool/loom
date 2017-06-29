package jobt.api.product;

import java.nio.file.Path;

public final class ResourcesTreeProduct implements Product {

    private final Path srcDir;

    public ResourcesTreeProduct(final Path srcDir) {
        this.srcDir = srcDir;
    }

    public Path getSrcDir() {
        return srcDir;
    }

}
