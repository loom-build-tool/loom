/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
