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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.util.Preconditions;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;

final class SpotBugsSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(SpotBugsSingleton.class);

    private static final Map<String, String> PLUGIN_JAR_PREFIXES = Map.of(
        "FbContrib", "fb-contrib",
        "FindSecBugs", "findsecbugs");

    private static volatile boolean initialized;

    private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

    private SpotBugsSingleton() {
    }

    static void initSpotBugs(final Set<String> plugins) {
        if (!initialized) {
            synchronized (SpotBugsSingleton.class) {
                if (!initialized) {
                    loadSpotBugsPlugin(plugins);
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
    private static void loadSpotBugsPlugin(final Set<String> plugins) {
        final Set<String> unknownPlugins = plugins.stream()
            .filter(pn -> !PLUGIN_JAR_PREFIXES.keySet().contains(pn))
            .collect(Collectors.toSet());

        Preconditions.checkState(unknownPlugins.isEmpty(),
            "Unknown SpotBugs plugin(s): %s", unknownPlugins);

        final ClassLoader contextClassLoader = SpotBugsSingleton.class.getClassLoader();

        final Set<String> pluginFilePrefixes = PLUGIN_JAR_PREFIXES.entrySet().stream()
            .filter(e -> plugins.contains(e.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toSet());

        LOG.debug("Requesting custom plugins by prefixes {}", pluginFilePrefixes);

        try {
            final List<Path> availablePluginJars =
                Collections.list(contextClassLoader.getResources("findbugs.xml")).stream()
                    .map(SpotBugsSingleton::normalizeUrl)
                    .collect(Collectors.toList());

            LOG.info("Available custom plugins: {}", availablePluginJars);

            for (final String prefix : pluginFilePrefixes) {
                addCustomPlugin(contextClassLoader, search(prefix, availablePluginJars));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URI search(final String prefix, final List<Path> availablePluginJars) {
        return availablePluginJars.stream()
            .filter(p -> p.getFileName().toString().startsWith(prefix))
            .findFirst()
            .map(Path::toUri)
            .orElseThrow(() -> new IllegalStateException(
                "Requested SpotBugs plugin not found by prefix " + prefix));
    }

    private static void addCustomPlugin(final ClassLoader contextClassLoader, final URI pluginUri) {
        LOG.debug("Loading custom plugin '{}'", pluginUri);
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
    }

    /**
     * jar:file:/C:/Users/leftout/plugin-spotbugs/findsecbugs-plugin-x.y.z.jar!/findbugs.xml
     * will become C:/Users/leftout/plugin-spotbugs/findsecbugs-plugin-x.y.z.jar
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
