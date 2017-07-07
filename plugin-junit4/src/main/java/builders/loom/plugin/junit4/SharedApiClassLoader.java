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

package builders.loom.plugin.junit4;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrap main loader to allow access to shared classes from sharedClassLoader.
 */
public final class SharedApiClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SharedApiClassLoader.class);

    // please add DOTs
    private static final List<String> WHITELISTED_PACKAGES =
        Arrays.asList("builders.loom.plugin.junit4.shared.");

    private final ClassLoader sharedClassLoader;

    public SharedApiClassLoader(
        final ClassLoader mainClassLoader, final ClassLoader sharedClassLoader) {
        super(mainClassLoader);
        this.sharedClassLoader = sharedClassLoader;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException notFound) {
            if (classAllowed(name)) {
                return sharedClassLoader.loadClass(name);
            } else {
                throw notFound;
            }
        }
    }

    private boolean classAllowed(final String className) {

        final boolean allowed = isWhitelistedPackage(className);

        LOG.trace("Class <{}> is {}", className, allowed ? "whitelisted" : "not whitelisted");

        return allowed;
    }

    private boolean isWhitelistedPackage(final String className) {
        return WHITELISTED_PACKAGES.stream().anyMatch(className::startsWith);
    }

}
