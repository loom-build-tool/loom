package builders.loom.plugin.java;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NullCacher implements FileCacher {

    @Override
    public boolean filesCached(final List<Path> srcPaths) throws IOException {
        return false;
    }

    @Override
    public void cacheFiles(final List<Path> srcPaths) throws IOException {
    }

}
