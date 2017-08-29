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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictedClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(RestrictedClassLoader.class);

    // please add DOTs
    private static final List<String> WHITELISTED_PACKAGES =
        List.of("builders.loom.plugin.junit4.shared.");

    public RestrictedClassLoader(final ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        if (!classAllowed(name)) {
            throw new IllegalStateException("Access to class not allowed: " + name);
        }
        return super.loadClass(name, resolve);
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
