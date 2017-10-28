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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public final class Hashing {

    private Hashing() {
    }

    public static String hash(final Collection<String> strings) {
        return new Hasher().putStrings(strings).hashHex();
    }

    public static String hash(final String... strings) {
        return new Hasher().putStrings(strings).hashHex();
    }

    public static String hash(final Path f) {
        final Hasher hasher = new Hasher();

        final byte[] buf = new byte[8192];
        int cnt;

        try (final InputStream in = Files.newInputStream(f)) {
            while ((cnt = in.read(buf)) != -1) {
                hasher.putBytes(buf, 0, cnt);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return hasher.hashHex();
    }

    public static byte[] hash(final byte[] data) {
        return new Hasher().putBytes(data).hash();
    }

}
