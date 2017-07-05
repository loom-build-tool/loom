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

package builders.loom.plugin;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load classes from extClassLoader and additionally from appClassLoader if whitelisted.
 */
public final class BiSectFilteringClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(BiSectFilteringClassLoader.class);

    // please add DOTs
    private static final List<String> WHITELISTED_PACKAGES =
        Arrays.asList("org.slf4j.", "loom.api.", "loom.util.");

    private final ClassLoader appClassLoader;

    public BiSectFilteringClassLoader(
        final ClassLoader extClassLoader, final ClassLoader appClassLoader) {
        super(extClassLoader);
        this.appClassLoader = appClassLoader;
        checkHierarchy(extClassLoader, appClassLoader);
    }

    private static void checkHierarchy(
        final ClassLoader extClassLoader, final ClassLoader appClassLoader) {

        boolean isOk = false;

        for (ClassLoader cl = appClassLoader; cl != null; cl = cl.getParent()) {
            if (cl.equals(extClassLoader)) {
                isOk = true;
            }
        }

        if (!isOk) {
            throw new IllegalArgumentException("ClassLoaders must be in one hierarchy");
        }

    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException notFound) {
            if (classAllowed(name)) {
                return appClassLoader.loadClass(name);
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
