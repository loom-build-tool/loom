package jobt.plugin.mavenresolver;

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
        final Path cacheFile = resolveCacheFile(cacheName);
        final Path parent = cacheFile.getParent();

        try {
            Files.createDirectories(parent);


            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(cacheFile, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)))) {


                out.writeObject(new CacheWrapper(cacheSignature, data));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveCacheFile(final String cacheName) {
        final Path cacheDir = Paths.get(".jobt", "cache", "mavenresolver");
        return cacheDir.resolve(cacheName + ".cache");
    }


    public T readCache() {
        final Path cacheFile = resolveCacheFile(cacheName);

        if (Files.notExists(cacheFile)) {
            return null;
        }

        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)))) {
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

}
