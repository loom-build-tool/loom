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

import java.beans.Statement;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.Constants;
import builders.loom.RuntimeConfigurationImpl;
import builders.loom.Version;
import builders.loom.api.Plugin;
import builders.loom.api.PluginSettings;
import builders.loom.config.BuildConfigWithSettings;
import builders.loom.util.SystemUtil;
import builders.loom.util.ThreadUtil;

@SuppressWarnings({
    "checkstyle:classfanoutcomplexity",
    "checkstyle:illegalcatch",
    "checkstyle:classdataabstractioncoupling"})
public class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    private static final Map<String, String> INTERNAL_PLUGINS;
    private static final Set<String> DEFAULT_PLUGINS;

    private final Path loomBaseDir = SystemUtil.determineLoomBaseDir();
    private final BuildConfigWithSettings buildConfig;
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final TaskRegistryLookup taskRegistry;
    private final ServiceLocatorImpl serviceLocator;
    private final Set<String> configuredPluginSettings = new CopyOnWriteArraySet<>();

    static {
        final Map<String, String> intPlugins = new HashMap<>();
        intPlugins.put("java", "builders.loom.plugin.java.JavaPlugin");
        intPlugins.put("junit4", "builders.loom.plugin.junit4.Junit4Plugin");
        intPlugins.put("mavenresolver", "builders.loom.plugin.mavenresolver.MavenResolverPlugin");
        intPlugins.put("checkstyle", "builders.loom.plugin.checkstyle.CheckstylePlugin");
        intPlugins.put("findbugs", "builders.loom.plugin.findbugs.FindbugsPlugin");
        intPlugins.put("pmd", "builders.loom.plugin.pmd.PmdPlugin");
        intPlugins.put("springboot", "builders.loom.plugin.springboot.SpringBootPlugin");
        intPlugins.put("idea", "builders.loom.plugin.idea.IdeaPlugin");
        intPlugins.put("eclipse", "builders.loom.plugin.eclipse.EclipsePlugin");
        INTERNAL_PLUGINS = Collections.unmodifiableMap(intPlugins);

        final Set<String> defaultPlugins = new HashSet<>();
        defaultPlugins.add("java");
        defaultPlugins.add("mavenresolver");
        DEFAULT_PLUGINS = Collections.unmodifiableSet(defaultPlugins);
    }

    public PluginRegistry(final BuildConfigWithSettings buildConfig,
                          final RuntimeConfigurationImpl runtimeConfiguration,
                          final TaskRegistryLookup taskRegistry,
                          final ServiceLocatorImpl serviceLocator) {

        this.buildConfig = buildConfig;
        this.runtimeConfiguration = runtimeConfiguration;
        this.taskRegistry = taskRegistry;
        this.serviceLocator = serviceLocator;
    }

    public void initPlugins() {
        final AtomicReference<Throwable> firstException = new AtomicReference<>();

        final Set<String> plugins = new HashSet<>(DEFAULT_PLUGINS);
        plugins.addAll(buildConfig.getPlugins());

        final ExecutorService executorService = Executors.newCachedThreadPool(
            ThreadUtil.newThreadFactory("plugin-init"));

        for (final String plugin : plugins) {
            if (firstException.get() != null) {
                break;
            }
            CompletableFuture.runAsync(() -> {
                    try {
                        initPlugin(plugin);
                    } catch (final Throwable e) {
                        executorService.shutdownNow();
                        firstException.compareAndSet(null, e);
                    }
                }, executorService
            );
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }

        if (firstException.get() != null) {
            throw new IllegalStateException(firstException.get());
        }

        validateConfiguredTasks();
        validateSettings();
    }

    private void initPlugin(final String pluginName) throws Exception {
        LOG.info("Initialize plugin {}", pluginName);

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
        final Plugin plugin = (Plugin) pluginClass.newInstance();
        plugin.setName(pluginName);
        plugin.setTaskRegistry(taskRegistry);
        plugin.setServiceLocator(serviceLocator);
        plugin.setBuildConfig(buildConfig);
        plugin.setRuntimeConfiguration(runtimeConfiguration);
        plugin.setRepositoryPath(Constants.PROJECT_LOOM_PATH.resolve(
            Paths.get(Version.getVersion(), pluginName)));
        injectPluginSettings(pluginName, plugin);
        plugin.configure();

        LOG.info("Plugin {} initialized", pluginName);
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

    private void injectPluginSettings(final String plugin, final Plugin regPlugin)
        throws Exception {

        final PluginSettings pluginSettings = regPlugin.getPluginSettings();

        if (pluginSettings == null) {
            return;
        }

        final Map<String, String> settings = buildConfig.getSettings();
        final List<String> properties = settings.keySet().stream()
            .filter(k -> k.startsWith(plugin + "."))
            .collect(Collectors.toList());

        for (final String property : properties) {
            final String propertyName = property.substring(plugin.length() + 1);
            final String setter = constructSetter(propertyName);
            final String[] setterArgs = {settings.get(property)};

            try {
                new Statement(pluginSettings, setter, setterArgs).execute();
            } catch (final NoSuchMethodException e) {
                throw new IllegalStateException(String.format("Plugin %s has no setting '%s'",
                    plugin, propertyName));
            }

            configuredPluginSettings.add(property);
        }
    }

    private static String constructSetter(final String propertyName) {
        return "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    private void validateConfiguredTasks() {
        final Set<String> providedProducts = taskRegistry.configuredTasks().stream()
            .map(ConfiguredTask::getProvidedProduct)
            .collect(Collectors.toSet());

        for (final ConfiguredTask configuredTask : taskRegistry.configuredTasks()) {
            final Set<String> usedProducts = configuredTask.getUsedProducts();
            for (final String usedProduct : usedProducts) {
                if (!providedProducts.contains(usedProduct)) {
                    throw new IllegalStateException("Task " + configuredTask.getName()
                        + " requests non existing product <" + usedProduct + ">");
                }
            }
        }
    }

    private void validateSettings() {
        final Set<String> unknownSettings = new HashSet<>(buildConfig.getSettings().keySet());
        unknownSettings.removeAll(configuredPluginSettings);

        if (!unknownSettings.isEmpty()) {
            LOG.warn("Unknown settings: " + unknownSettings);
        }
    }

}
