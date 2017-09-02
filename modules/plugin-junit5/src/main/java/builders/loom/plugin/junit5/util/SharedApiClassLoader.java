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

package builders.loom.plugin.junit5.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap main loader to allow access to shared classes from sharedApiClassLoader.
 */
public final class SharedApiClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SharedApiClassLoader.class);

    // please add DOTs
    private static final List<String> API_PACKAGES = List.of(
        "builders.loom.plugin.junit5.shared."
    );

    private final ClassLoader sharedClassLoader;

    public SharedApiClassLoader(
        final ClassLoader mainClassLoader, final ClassLoader sharedApiClassLoader) {
        super(mainClassLoader);
        this.sharedClassLoader = sharedApiClassLoader;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        final boolean isApiClass = isApiClass(name);
        try {
            final Class<?> fromParent = super.loadClass(name, resolve);
            if (isApiClass) {
                throw new IllegalStateException(
                    "Api class must not exist in parent classloader: " + name);
            }
            return fromParent;
        } catch (final ClassNotFoundException notFound) {
            if (isApiClass) {
                return sharedClassLoader.loadClass(name);
            }
            throw notFound;
        }
    }

    private boolean isApiClass(final String className) {

        final boolean allowed = isWhitelistedPackage(className);

        LOG.trace("Class <{}> is {}", className, allowed ? "whitelisted" : "not whitelisted");

        return allowed;
    }

    private boolean isWhitelistedPackage(final String className) {
        return API_PACKAGES.stream().anyMatch(className::startsWith);
    }

}
