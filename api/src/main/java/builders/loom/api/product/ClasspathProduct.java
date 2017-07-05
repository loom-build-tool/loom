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

package builders.loom.api.product;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Must be immutable!
 */
public final class ClasspathProduct implements Product {

    private final List<Path> entries;

    public ClasspathProduct(final Path entry) {
        Objects.requireNonNull(entry);
        this.entries = Collections.unmodifiableList(
            Collections.singletonList(entry));
    }

    public ClasspathProduct(final List<Path> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<Path> getEntries() {
        return entries;
    }

    public URL[] getEntriesAsUrlArray() {
        return
            entries.stream()
                .map(ClasspathProduct::toURL)
                .toArray(URL[]::new);
    }

    public List<URL> getEntriesAsUrls() {
        return
            entries.stream()
                .map(ClasspathProduct::toURL)
                .collect(Collectors.toList());
    }

    private static URL toURL(final Path p) {
        try {
            return p.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Path getSingleEntry() {
        if (entries.size() != 1) {
            throw new IllegalStateException();
        }
        return entries.get(0);
    }

    @Override
    public String toString() {
        return entries.toString();
    }

}
