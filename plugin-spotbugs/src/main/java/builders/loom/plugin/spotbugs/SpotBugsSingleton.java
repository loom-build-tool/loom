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

package builders.loom.plugin.spotbugs;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;

public final class SpotBugsSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(SpotBugsSingleton.class);

    private static volatile boolean initialized;

    private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

    private SpotBugsSingleton() {
    }

    public static void initSpotBugs(final boolean loadFbContrib, final boolean loadFindBugsSec) {

        if (!initialized) {
            synchronized (SpotBugsSingleton.class) {
                if (!initialized) {

                    loadSpotBugsPlugin(loadFbContrib, loadFindBugsSec);
                    disableUpdateChecksOnEveryPlugin();

                    LOG.info("Using SpotBugs plugins: {}", Plugin.getAllPluginIds());

                    initialized = true;

                }
            }
        }

    }

    /**
     * Note: spotbugs plugins are registered in a static map and thus has many concurrency issues.
     */
    private static void loadSpotBugsPlugin(
        final boolean loadFbContrib, final boolean loadFindBugsSec) {

        final ClassLoader contextClassLoader = SpotBugsSingleton.class.getClassLoader();

        try {

            Collections.list(contextClassLoader.getResources("findbugs.xml")).stream()
                .map(SpotBugsSingleton::normalizeUrl)
                .filter(Files::exists)
                .filter(file ->
                    loadFbContrib && file.getFileName().toString().startsWith("fb-contrib")
                    || loadFindBugsSec && file.getFileName().toString().startsWith("findsecbugs")
                )
                .map(Path::toUri)
                .forEach(pluginUri -> {
                    try {
                        Plugin.addCustomPlugin(pluginUri, contextClassLoader);
                    } catch (final PluginException e) {
                        throw new IllegalStateException("Error loading plugin " + pluginUri, e);
                    } catch (final DuplicatePluginIdException e) {
                        if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
                            throw new IllegalStateException(
                                "Duplicate SpotBugs plugin " + e.getPluginId());
                        }
                    }
                });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    /**
     * jar:file:/C:/Users/leftout/plugin-spotbugs/spotbugs-3.0.1.jar!/spotbugs.xml
     * will become C:/Users/leftout/plugin-spotbugs/spotbugs-3.0.1.jar
     * .
     */
    private static Path normalizeUrl(final URL url) {
        try {
            return Paths.get(new URL(url.getPath().split("!")[0]).toURI());
        } catch (final MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException("Unable to normalize url: " + url);
        }
    }

    /**
     * Disable the update check for every plugin.
     * See http://findbugs.sourceforge.net/updateChecking.html.
     */
    private static void disableUpdateChecksOnEveryPlugin() {
        for (final Plugin plugin : Plugin.getAllPlugins()) {
            plugin.setMyGlobalOption("noUpdateChecks", "true");
        }
    }

}
