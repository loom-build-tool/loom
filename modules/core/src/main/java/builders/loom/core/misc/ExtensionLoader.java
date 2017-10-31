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

package builders.loom.core.misc;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.core.LoomVersion;
import builders.loom.core.plugin.BiSectFilteringClassLoader;
import builders.loom.util.ClassLoaderUtil;

public final class ExtensionLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionLoader.class);

    private ExtensionLoader() {
    }

    public static Class<?> loadExtension(final Path baseDir, final String extensionName,
                                         final String extensionClassname) {

        final URL pluginJarUrl = findExtensionUrl(baseDir, extensionName);

        // Note that plugin dependencies are specified in MANIFEST.MF
        // @link https://docs.oracle.com/javase/tutorial/deployment/jar/downman.html

        final URLClassLoader classLoader = new URLClassLoader(
            new URL[]{pluginJarUrl},
            new BiSectFilteringClassLoader(
                getPlatformClassLoader(),
                Thread.currentThread().getContextClassLoader()
            ));

        final Class<?> pluginClass;
        try {
            pluginClass = classLoader.loadClass(extensionClassname);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(String.format(
                "Couldn't load extension %s from jar file %s", extensionName, pluginJarUrl), e);
        }

        LOG.debug("Loaded extension {} from {}", extensionName, pluginJarUrl);

        return pluginClass;
    }

    private static URL findExtensionUrl(final Path baseDir, final String name) {
        final String loomVersion = LoomVersion.getVersion();
        final Path libraryPath = baseDir.resolve(Paths.get("library", "loom-" + loomVersion));
        final Path pluginDir = libraryPath.resolve(name);
        final Path pluginFile =
            pluginDir.resolve(String.format("loom-%s-%s.jar", name, loomVersion));

        return ClassLoaderUtil.toUrl(pluginFile);
    }

    private static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }

}
