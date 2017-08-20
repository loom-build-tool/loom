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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TempFile implements Closeable {

    private final Path file;

    public TempFile(final Path tmpPath, final String prefix, final String suffix) {
        try {
            file = Files.createTempFile(tmpPath, prefix, suffix);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(file);
    }

    @Override
    public String toString() {
        return file.toString();
    }

}
