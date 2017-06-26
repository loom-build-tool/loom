package jobt.api;

import java.nio.file.Path;

public final class ResourcesTree implements Product {

    private final Path srcDir;

    public ResourcesTree(final Path srcDir) {
        this.srcDir = srcDir;
    }

    public Path getSrcDir() {
        return srcDir;
    }

}
