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

package builders.loom.plugin.junit4.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Predicate;

import builders.loom.util.Util;

/**
 * Wrap parent classloader enriching it with classes loader from extraClassesLoader.
 *
 */
public class InjectingClassLoader extends ClassLoader {

    private final ClassLoader extraClassesLoader;
    private final Predicate<String> injectPredicate;

    public InjectingClassLoader(
        final ClassLoader parent, final ClassLoader extraClassesLoader,
        final Predicate<String> injectPredicate) {
        super(parent);
        this.injectPredicate = injectPredicate;
        Objects.requireNonNull(parent);
        Objects.requireNonNull(extraClassesLoader);
        this.extraClassesLoader = extraClassesLoader;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (!injectPredicate.test(name)) {
            return super.loadClass(name);
        }

        try (InputStream in = extraClassesLoader.getResourceAsStream(
            Util.resourceNameFromClassName(name))) {

            Objects.requireNonNull(in, "Failed to load class for injection: " + name);
            final byte[] classBytes = Util.toByteArray(in);
            return defineClass(name, classBytes, 0, classBytes.length);

        } catch (final IOException e) {
            throw new ClassNotFoundException();
        }
    }
}
