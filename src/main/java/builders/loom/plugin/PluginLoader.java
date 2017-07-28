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

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.RuntimeConfigurationImpl;
import builders.loom.Version;
import builders.loom.api.BuildConfig;
import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.LoomPaths;
import builders.loom.api.Plugin;
import builders.loom.api.PluginSettings;
import builders.loom.util.BeanUtil;
import builders.loom.util.SystemUtil;

@SuppressWarnings({
    "checkstyle:classfanoutcomplexity",
    "checkstyle:illegalcatch",
    "checkstyle:classdataabstractioncoupling"})
public class PluginLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    // FIXMzz
    private static final Map<String, String> INTERNAL_GLOBAL_PLUGINS = Map.of(
        "idea", "builders.loom.plugin.idea.IdeaPlugin",
        "eclipse", "builders.loom.plugin.eclipse.EclipsePlugin"
    );

    private static final Map<String, String> INTERNAL_MODULE_PLUGINS = Map.of(
        "java", "builders.loom.plugin.java.JavaPlugin",
        "junit4", "builders.loom.plugin.junit4.JUnit4Plugin",
        "mavenresolver", "builders.loom.plugin.mavenresolver.MavenResolverPlugin",
        "checkstyle", "builders.loom.plugin.checkstyle.CheckstylePlugin",
        "findbugs", "builders.loom.plugin.findbugs.FindbugsPlugin",
        "pmd", "builders.loom.plugin.pmd.PmdPlugin",
        "springboot", "builders.loom.plugin.springboot.SpringBootPlugin",
        "idea", "builders.loom.plugin.idea.IdeaPlugin",
        "eclipse", "builders.loom.plugin.eclipse.EclipsePlugin"
    );

    private final Path loomBaseDir = SystemUtil.determineLoomBaseDir();
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final Map<String, Class<?>> pluginClasses = new HashMap<>();

    public PluginLoader(final RuntimeConfigurationImpl runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public void initPlugins(final Set<String> pluginsToInitialize,
                            final BuildConfig moduleConfig,
                            final TaskRegistryImpl taskRegistry,
                            final ServiceLocatorImpl serviceLocator) {

        final Set<String> acceptedSettings = new HashSet<>();
        for (final String plugin : pluginsToInitialize) {
            acceptedSettings.addAll(initPlugin(plugin, moduleConfig, taskRegistry, serviceLocator));
        }

        validateConfiguredTasks(taskRegistry);

        if (moduleConfig instanceof BuildConfigWithSettings) {
            validateSettings((BuildConfigWithSettings) moduleConfig, acceptedSettings);
        }
    }

    private Set<String> initPlugin(final String pluginName, final BuildConfig config,
                                   final TaskRegistryImpl taskRegistry,
                                   final ServiceLocatorImpl serviceLocator) {

        final Plugin plugin = getPlugin(pluginName);

        plugin.setName(pluginName);
        plugin.setTaskRegistry(taskRegistry);
        plugin.setServiceLocator(serviceLocator);
        plugin.setRuntimeConfiguration(runtimeConfiguration);
        plugin.setRepositoryPath(LoomPaths.PROJECT_LOOM_PATH.resolve(
            Paths.get(Version.getVersion(), pluginName)));

        final Set<String> acceptedSettings = injectPluginSettings(pluginName, plugin, config);

        plugin.configure();

        LOG.info("Plugin {} initialized", pluginName);

        return acceptedSettings;
    }

    private Plugin getPlugin(final String pluginName) {
        final Class<?> pluginClass = pluginClasses.computeIfAbsent(pluginName, this::loadPlugin);

        try {
            return (Plugin) pluginClass.getDeclaredConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException
            | InvocationTargetException e) {
            throw new IllegalStateException("Error initializing Plugin " + pluginName, e);
        }
    }

    private Class<?> loadPlugin(final String pluginName) {
        final String pluginClassname = INTERNAL_MODULE_PLUGINS.get(pluginName);
        if (pluginClassname == null) {
            throw new IllegalArgumentException("Unknown plugin: " + pluginName);
        }

        final URL pluginJarUrl = findPluginUrl(pluginName);

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
            pluginClass = classLoader.loadClass(pluginClassname);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(String
                .format("Couldn't load plugin %s from jar file %s", pluginName, pluginJarUrl), e);
        }

        LOG.debug("Loaded plugin {} from {}", pluginName, pluginJarUrl);

        return pluginClass;
    }

    private URL findPluginUrl(final String name) {
        final String loomVersion = Version.getVersion();
        final Path libraryPath = loomBaseDir.resolve(Paths.get("library", "loom-" + loomVersion));
        final Path pluginDir = libraryPath.resolve("plugin-" + name);
        return buildUrl(pluginDir.resolve(String.format("plugin-%s-%s.jar", name, loomVersion)));
    }

    private static URL buildUrl(final Path f) {
        try {
            return f.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }

    private Set<String> injectPluginSettings(final String plugin, final Plugin regPlugin,
                                             final BuildConfig moduleConfig) {

        final PluginSettings pluginSettings = regPlugin.getPluginSettings();

        if (pluginSettings == null) {
            return Collections.emptySet();
        }

        final Set<String> configuredPluginSettings = new HashSet<>();

        if (moduleConfig instanceof BuildConfigWithSettings) {
            final BuildConfigWithSettings moduleConfigWithSettings =
                (BuildConfigWithSettings) moduleConfig;
            final Map<String, String> settings = moduleConfigWithSettings.getSettings();
            final List<String> properties = settings.keySet().stream()
                .filter(k -> k.startsWith(plugin + "."))
                .collect(Collectors.toList());

            for (final String property : properties) {
                final String propertyName = property.substring(plugin.length() + 1);
                final String propertyValue = settings.get(property);

                try {
                    BeanUtil.set(pluginSettings, propertyName, propertyValue);
                } catch (final Exception e) {
                    throw new IllegalStateException("Error injecting settings into plugin "
                        + plugin, e);
                }

                configuredPluginSettings.add(property);
            }
        }

        return configuredPluginSettings;
    }

    private void validateConfiguredTasks(final TaskRegistryLookup taskRegistry) {
        final Set<String> providedProducts = taskRegistry.configuredTasks().stream()
            .map(ConfiguredTask::getProvidedProduct)
            .collect(Collectors.toSet());

        for (final ConfiguredTask configuredTask : taskRegistry.configuredTasks()) {
            final Set<String> usedProducts = configuredTask.getUsedProducts();
            for (final String usedProduct : usedProducts) {
                if (!providedProducts.contains(usedProduct)) {
                    throw new IllegalStateException(
                        String.format("Task %s requests non existing product <%s>",
                            configuredTask.getName(), usedProduct));
                }
            }
        }
    }

    private void validateSettings(final BuildConfigWithSettings moduleConfig,
                                  final Set<String> configuredPluginSettings) {
        final Set<String> unknownSettings = new HashSet<>(moduleConfig.getSettings().keySet());
        unknownSettings.removeAll(configuredPluginSettings);

        if (!unknownSettings.isEmpty()) {
            LOG.warn("Unknown settings: " + unknownSettings);
        }
    }

}
