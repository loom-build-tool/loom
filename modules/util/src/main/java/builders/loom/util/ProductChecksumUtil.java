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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class ProductChecksumUtil {

    public static String recursiveContentChecksum(final Path path) {
        try {
            final Stream<Path> pathStream =
                Files.find(path, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile());

            return contentChecksum(pathStream);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String contentChecksum(final Stream<Path> files) {
        final Hasher hasher = new Hasher();
        files
            .sorted(Comparator.comparing(Path::toString))
            .flatMap(p -> Stream.of(new Hasher().putFile(p).hash()))
            .forEach(hasher::putBytes);

        return hasher.hashHex();
    }

    public static String recursiveMetaChecksum(final Path path) {
        final Hasher hasher = new Hasher();

        try {
            Files.find(path, Integer.MAX_VALUE, (p, attr) -> true)
                .sorted(Comparator.comparing(Path::toString))
                .flatMap(ProductChecksumUtil::hashMeta)
                .forEach(hasher::putString);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return hasher.hashHex();
    }

    public static String metaChecksum(final List<Path> srcFiles) {
        final Hasher hasher = new Hasher();

        srcFiles.stream()
            .sorted(Comparator.comparing(Path::toString))
            .flatMap(ProductChecksumUtil::hashMeta)
            .forEach(hasher::putString);

        return hasher.hashHex();
    }

    private static Stream<String> hashMeta(final Path f) {
        try {
            return Stream.of(f.toString(), Long.toString(Files.size(f)),
                Long.toString(Files.getLastModifiedTime(f).toMillis()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String checksum(final Map<String, List<String>> properties) {
        final List<String> strings = new ArrayList<>();

        properties.keySet().stream().sorted().forEach(k -> {
            strings.add(k);
            strings.addAll(properties.get(k));
        });

        return Hashing.hash(strings);
    }

    public static String random() {
        return UUID.randomUUID().toString();
    }

}
