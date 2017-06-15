package jobt.plugin.java;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DiskKeyValueCache implements KeyValueCache {

    private final Path cacheFile;
    private final Map<String, Long> cache;
    private boolean dirty;

    public DiskKeyValueCache(final Path cacheFile) {
        this.cacheFile = Objects.requireNonNull(cacheFile, "cacheFile must not be null");
        cache = Files.exists(cacheFile) ? loadCache(cacheFile) : new HashMap<>();
    }

    private static Map<String, Long> loadCache(final Path cacheFile) {
        try (final BufferedReader reader =
                 Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            return reader.lines()
                .map(KeyValue::new)
                .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void saveCache() {
        if (!dirty) {
            return;
        }

        final Path cacheTmpFile = Paths.get(cacheFile + ".tmp");

        final Path parent = cacheTmpFile.getParent();
        if (parent == null) {
            // Keep Findbugs happy
            throw new IllegalStateException("Parent of " + cacheTmpFile + " is null");
        }

        try {
            Files.createDirectories(parent);

            try (final BufferedWriter writer =
                     Files.newBufferedWriter(cacheTmpFile, StandardCharsets.UTF_8)) {
                for (final Map.Entry<String, Long> entry : cache.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write('\t');
                    writer.write(entry.getValue().toString());
                    writer.write('\n');
                }
            }

            Files.move(cacheTmpFile, cacheFile,
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Long get(final String key) {
        return cache.get(key);
    }

    @Override
    public void put(final String key, final Long value) {
        cache.put(key, value);
        dirty = true;
    }

    @Override
    public void remove(final String key) {
        cache.remove(key);
        dirty = true;
    }

    private static final class KeyValue {

        private final String key;
        private final Long value;

        KeyValue(final String line) {
            final String[] split = line.split("\t");
            key = split[0];
            value = Long.valueOf(split[1]);
        }

        String getKey() {
            return key;
        }

        Long getValue() {
            return value;
        }

    }

}
