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

import static java.util.Map.entry;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.RuntimeConfigurationImpl;
import builders.loom.Version;
import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.LoomPaths;
import builders.loom.api.Plugin;
import builders.loom.api.PluginSettings;
import builders.loom.util.SystemUtil;

@SuppressWarnings({
    "checkstyle:classfanoutcomplexity",
    "checkstyle:illegalcatch",
    "checkstyle:classdataabstractioncoupling"})
public class PluginLoader {

    private static final Logger LOG = LoggerFactory.getLogger(PluginLoader.class);

    private static final Map<String, String> INTERNAL_PLUGINS = Map.ofEntries(
        entry("java", "builders.loom.plugin.java.JavaPlugin"),
        entry("junit4", "builders.loom.plugin.junit4.JUnit4Plugin"),
        entry("mavenresolver", "builders.loom.plugin.mavenresolver.MavenResolverPlugin"),
        entry("checkstyle", "builders.loom.plugin.checkstyle.CheckstylePlugin"),
        entry("findbugs", "builders.loom.plugin.findbugs.FindbugsPlugin"),
        entry("pmd", "builders.loom.plugin.pmd.PmdPlugin"),
        entry("springboot", "builders.loom.plugin.springboot.SpringBootPlugin"),
        entry("idea", "builders.loom.plugin.idea.IdeaPlugin"),
        entry("eclipse", "builders.loom.plugin.eclipse.EclipsePlugin")
    );

    private final Path loomBaseDir = SystemUtil.determineLoomBaseDir();
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final Map<String, Class<?>> pluginClasses = new HashMap<>();

    public PluginLoader(final RuntimeConfigurationImpl runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public void initPlugins(final Set<String> pluginNames,
                            final BuildConfigWithSettings moduleConfig,
                            final TaskRegistryImpl taskRegistry,
                            final ServiceLocatorImpl serviceLocator) {

        final Set<String> acceptedSettings = new HashSet<>();
        for (final String plugin : pluginNames) {
            acceptedSettings.addAll(initPlugin(plugin, moduleConfig, taskRegistry, serviceLocator));
        }

        validateConfiguredTasks(taskRegistry);
        validateSettings(moduleConfig, acceptedSettings);
    }

    private Set<String> initPlugin(final String pluginName, final BuildConfigWithSettings config,
                                   final TaskRegistryImpl taskRegistry,
                                   final ServiceLocatorImpl serviceLocator) {

        LOG.info("Initialize plugin {}", pluginName);

        final Plugin plugin = getPlugin(pluginName);

        plugin.setName(pluginName);
        plugin.setTaskRegistry(taskRegistry);
        plugin.setServiceLocator(serviceLocator);
        plugin.setModuleConfig(config);
        plugin.setRuntimeConfiguration(runtimeConfiguration);
        plugin.setRepositoryPath(LoomPaths.PROJECT_LOOM_PATH.resolve(
            Paths.get(Version.getVersion(), pluginName)));

        final Set<String> acceptedSettings = injectPluginSettings(pluginName, plugin, config);

        plugin.configure();

        LOG.info("Plugin {} initialized", pluginName);

        return acceptedSettings;
    }

    private Plugin getPlugin(final String pluginName) {
        final Class<?> pluginClass = pluginClasses.computeIfAbsent(pluginName, (p) -> {
            try {
                return loadPlugin(p);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });

        try {
            return (Plugin) pluginClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
            | InvocationTargetException e) {
            throw new IllegalStateException("Error initializing Plugin " + pluginName, e);
        }
    }

    private Class<?> loadPlugin(final String pluginName) throws ClassNotFoundException {
        LOG.info("Load plugin {}", pluginName);

        final String pluginClassname = INTERNAL_PLUGINS.get(pluginName);
        if (pluginClassname == null) {
            throw new IllegalArgumentException("Unknown plugin: " + pluginName);
        }

        final URL pluginJarUrl = findPluginUrl(pluginName);

        LOG.info("Load plugin {} using jar file from {}", pluginName, pluginJarUrl);

        // Note that plugin dependencies are specified in MANIFEST.MF
        // @link https://docs.oracle.com/javase/tutorial/deployment/jar/downman.html

        final URLClassLoader classLoader = new URLClassLoader(
            new URL[] {pluginJarUrl},
            new BiSectFilteringClassLoader(
                getPlatformClassLoader(),
                Thread.currentThread().getContextClassLoader()
            ));

        final Class<?> pluginClass = classLoader.loadClass(pluginClassname);

        LOG.info("Plugin {} loaded", pluginName);

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
                                             final BuildConfigWithSettings moduleConfig) {

        final PluginSettings pluginSettings = regPlugin.getPluginSettings();

        if (pluginSettings == null) {
            return Collections.emptySet();
        }

        final PropertyDescriptor[] propertyDescriptors =
            getPropertyDescriptors(plugin, pluginSettings);

        final Set<String> configuredPluginSettings = new HashSet<>();

        final Map<String, String> settings = moduleConfig.getSettings();
        final List<String> properties = settings.keySet().stream()
            .filter(k -> k.startsWith(plugin + "."))
            .collect(Collectors.toList());

        for (final String property : properties) {
            final String propertyName = property.substring(plugin.length() + 1);
            final String propertyValue = settings.get(property);

            final Method setter = findSetter(propertyDescriptors, propertyName)
                .orElseThrow(() -> new IllegalStateException(
                    String.format("No property %s found in plugin %s", propertyName, plugin)));

            try {
                setter.invoke(pluginSettings, propertyValue);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(
                    String.format("Error calling %s with args %s on plugin %s",
                        setter, propertyValue, plugin), e);
            }

            configuredPluginSettings.add(property);
        }

        return configuredPluginSettings;
    }

    private static PropertyDescriptor[] getPropertyDescriptors(
        final String plugin, final PluginSettings pluginSettings) {

        final BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(pluginSettings.getClass());
        } catch (final IntrospectionException e) {
            throw new IllegalStateException("Can't inspect plugin " + plugin, e);
        }
        return beanInfo.getPropertyDescriptors();
    }

    private static Optional<Method> findSetter(final PropertyDescriptor[] propertyDescriptors,
                                               final String propertyName) {
        return Arrays.stream(propertyDescriptors)
            .filter(pd -> pd.getName().equals(propertyName))
            .findFirst()
            .map(PropertyDescriptor::getWriteMethod);
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
