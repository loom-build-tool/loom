package builders.loom.plugin.java;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileCacher {

    boolean filesCached(List<Path> srcPaths) throws IOException;

    void cacheFiles(List<Path> srcPaths) throws IOException;

}
