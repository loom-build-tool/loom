package jobt.api;

import java.nio.file.Path;

public final class SourceTree implements Product {

    private final Path srcDir;

    public SourceTree(final Path srcDir) {
        this.srcDir = srcDir;
    }

    public Path getSrcDir() {
        return srcDir;
    }

}
