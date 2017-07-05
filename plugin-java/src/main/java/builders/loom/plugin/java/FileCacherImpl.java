package builders.loom.plugin.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCacherImpl implements FileCacher {

    private final Path cacheFile;

    public FileCacherImpl(final String type) throws IOException {
        final Path path = Paths.get(".loom/", "cache", "java");
        Files.createDirectories(path);
        cacheFile = path.resolve("java-" + type + ".cache");
    }

    @Override
    @SuppressWarnings("checkstyle:returncount")
    public boolean filesCached(final List<Path> srcPaths) throws IOException {
        final Map<String, Long> cachedFiles = readCache();

        if (srcPaths.size() != cachedFiles.size()) {
            return false;
        }

        for (final Path srcPath : srcPaths) {
            final Long cachedMtime = cachedFiles.remove(srcPath.toString());
            if (cachedMtime == null) {
                return false;
            }
            final FileTime lastModifiedTime = Files.getLastModifiedTime(srcPath);
            if (!cachedMtime.equals(lastModifiedTime.toMillis())) {
                return false;
            }
        }

        return cachedFiles.isEmpty();
    }

    private Map<String, Long> readCache() throws IOException {
        if (Files.notExists(cacheFile)) {
            return Collections.emptyMap();
        }

        final List<String> strings = Files.readAllLines(cacheFile);
        return strings.stream()
            .map(s -> s.split("\t"))
            .collect(Collectors.toMap(split -> split[0], split -> Long.valueOf(split[1])));
    }

    @Override
    public void cacheFiles(final List<Path> srcPaths) throws IOException {
        final Map<String, Long> cachedFiles = new HashMap<>();

        for (final Path srcFile : srcPaths) {
            cachedFiles.put(srcFile.toString(), Files.getLastModifiedTime(srcFile).toMillis());
        }

        final List<String> stringStream = cachedFiles.entrySet().stream()
            .map(e -> e.getKey() + "\t" + e.getValue())
            .collect(Collectors.toList());

        Files.write(cacheFile, stringStream);
    }

}
