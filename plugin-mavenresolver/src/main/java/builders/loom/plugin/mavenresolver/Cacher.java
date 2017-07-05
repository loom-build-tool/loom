package builders.loom.plugin.mavenresolver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Cacher<T extends Serializable> {

    private final String cacheName;
    private final String cacheSignature;

    public Cacher(final String cacheName, final String cacheSignature) {
        this.cacheName = cacheName;
        this.cacheSignature = cacheSignature;
    }

    public void writeCache(final T data) {
        try {
            final Path cacheDir = Files.createDirectories(resolveCacheDir());
            final Path cacheFile = resolveCacheFile(cacheDir);

            try (ObjectOutputStream out = newOut(cacheFile)) {
                out.writeObject(new CacheWrapper<>(cacheSignature, data));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ObjectOutputStream newOut(final Path cacheFile) throws IOException {
        return new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(cacheFile,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)));
    }

    private Path resolveCacheDir() {
        return Paths.get(".loom", "cache", "mavenresolver");
    }

    private Path resolveCacheFile(final Path cacheDir) {
        return cacheDir.resolve(cacheName + ".cache");
    }

    public T readCache() {
        final Path cacheFile = resolveCacheFile(resolveCacheDir());

        if (Files.notExists(cacheFile)) {
            return null;
        }

        try (ObjectInputStream in = newIn(cacheFile)) {
            final CacheWrapper<T> cw = (CacheWrapper) in.readObject();

            if (cw.getSignature().equals(cacheSignature)) {
                return cw.getData();
            }

            return null;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private ObjectInputStream newIn(final Path cacheFile) throws IOException {
        return new ObjectInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)));
    }

}
