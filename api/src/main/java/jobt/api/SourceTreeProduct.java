package jobt.api;

import java.nio.file.Path;

public final class SourceTreeProduct implements Product {

    private final Path srcDir;

    public SourceTreeProduct(final Path srcDir) {
        this.srcDir = srcDir;
    }

    public Path getSrcDir() {
        return srcDir;
    }

}
