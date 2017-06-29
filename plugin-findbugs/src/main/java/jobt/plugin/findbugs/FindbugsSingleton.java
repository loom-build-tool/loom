package jobt.plugin.findbugs;

import java.io.IOException;
import java.io.UncheckedIOException;
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

public final class FindbugsSingleton {

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsSingleton.class);

    private static volatile boolean initialized;

    private static final String FINDBUGS_CORE_PLUGIN_ID = "edu.umd.cs.findbugs.plugins.core";

    private FindbugsSingleton() {
    }

    public static void initFindbugs() {

        if (!initialized) {
            synchronized (FindbugsSingleton.class) {
                if (!initialized) {

                    loadFindbugsPlugin();
                    disableUpdateChecksOnEveryPlugin();

                    LOG.info("Using findbugs plugins: {}", Plugin.getAllPluginIds());

                    initialized = true;

                }
            }
        }

    }

    /**
     * Note: findbugs plugins are registered in a static map and thus has many concurrency issues.
     */
    private static void loadFindbugsPlugin() {

        final ClassLoader contextClassLoader = FindbugsSingleton.class.getClassLoader();

        try {

            Collections.list(contextClassLoader.getResources("findbugs.xml")).stream()
                .map(FindbugsSingleton::normalizeUrl)
                .map(Paths::get)
                .filter(Files::exists)
                .map(Path::toUri)
                .forEach(pluginUri -> {
                    try {
                        Plugin.addCustomPlugin(pluginUri, contextClassLoader);
                    } catch (final PluginException e) {
                        throw new IllegalStateException("Error loading plugin " + pluginUri, e);
                    } catch (final DuplicatePluginIdException e) {
                        if (!FINDBUGS_CORE_PLUGIN_ID.equals(e.getPluginId())) {
                            throw new IllegalStateException(
                                "Duplicate findbugs plugin " + e.getPluginId());
                        }
                    }
                });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    public static String normalizeUrl(final URL url) {
        return url.getPath().split("!")[0].replace("file:", "");
    }

    /**
     * Disable the update check for every plugin. See http://findbugs.sourceforge.net/updateChecking.html
     */
    private static void disableUpdateChecksOnEveryPlugin() {
        for (final Plugin plugin : Plugin.getAllPlugins()) {
            plugin.setMyGlobalOption("noUpdateChecks", "true");
        }
    }

}
