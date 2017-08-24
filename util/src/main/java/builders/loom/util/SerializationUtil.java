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

package builders.loom.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public final class SerializationUtil {

    private SerializationUtil() {
    }

    public static void write(final Object data, final Path file) {
        try {
            Files.createDirectories(file.getParent());

            final Path tmpFile = Paths.get(file.toString() + ".tmp");

            try (ObjectOutputStream out = newOut(tmpFile)) {
                out.writeObject(data);
            }

            Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ObjectOutputStream newOut(final Path cacheFile) throws IOException {
        return new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(cacheFile)));
    }

    public static <T> Optional<T> readCache(final Class<T> clazz, final Path file) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        try (ObjectInputStream in = newIn(file)) {
            return Optional.of(clazz.cast(in.readObject()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ObjectInputStream newIn(final Path cacheFile) throws IOException {
        return new ObjectInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)));
    }

}
