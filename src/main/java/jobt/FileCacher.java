package jobt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCacher {

    private Path cacheFile;
    private Map<String, Long> cachedFiles = new HashMap<>();

    public FileCacher() throws IOException {
        final Path path = Paths.get(".jobt");
        if (Files.notExists(path)) {
            Files.createDirectory(path);
        }
        cacheFile = Paths.get(".jobt/file.cache");
        if (Files.exists(cacheFile)) {
            final List<String> strings = Files.readAllLines(cacheFile);
            for (final String string : strings) {
                final String[] split = string.split("\t");
                cachedFiles.put(split[0], Long.valueOf(split[1]));
            }
        }
    }

    public boolean notCached(final Path f) {
        final Long lastMod = cachedFiles.get(f.toString());
        try {
            return lastMod == null || Files.getLastModifiedTime(f).toMillis() != lastMod;
        } catch (final IOException e) {
            return true;
        }
    }

    public void cacheFiles(final List<Path> srcFiles) throws IOException {
        for (final Path srcFile : srcFiles) {
            cachedFiles.put(srcFile.toString(), Files.getLastModifiedTime(srcFile).toMillis());
        }

        final List<String> stringStream = cachedFiles.entrySet().stream()
            .map(e -> e.getKey() + "\t" + e.getValue())
            .collect(Collectors.toList());

        Files.write(cacheFile, stringStream);
    }

    public boolean isCacheEmpty() {
        return cachedFiles.isEmpty();
    }

}
