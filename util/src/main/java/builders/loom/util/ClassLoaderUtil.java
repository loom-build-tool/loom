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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ClassLoaderUtil {

    private ClassLoaderUtil() {
    }

    public static URL toUrl(final Path file) {
        if (file == null) {
            return null;
        }
        try {
            return file.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String classnameFromFilename(final String filename) {
        Objects.requireNonNull(filename);
        return
            StreamSupport.stream(
                Paths.get(filename.replaceFirst("\\.class$", "")).spliterator(), false)
            .map(Path::toString)
            .collect(Collectors.joining("."));
    }

    public static String resourceNameFromClassName(final String className) {
        Objects.requireNonNull(className);
        return className.replace('.', '/') + ".class";
    }

    public static <CT extends ClassLoader> CT privileged(
        final Supplier<CT> classLoaderSupplier) {
        return
            AccessController.doPrivileged(new PrivilegedAction<CT>() {
                @Override
                public CT run() {
                    return classLoaderSupplier.get();
                }
            });
    }

}
