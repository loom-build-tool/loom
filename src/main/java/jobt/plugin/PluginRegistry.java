package jobt.plugin;

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

import jobt.RuntimeConfigurationImpl;
import jobt.Version;
import jobt.api.Plugin;
import jobt.api.PluginSettings;
import jobt.api.TaskRegistry;
import jobt.config.BuildConfigWithSettings;
import jobt.util.ThreadUtil;

@SuppressWarnings({
    "checkstyle:classfanoutcomplexity",
    "checkstyle:illegalcatch",
    "checkstyle:classdataabstractioncoupling"})
public class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    private static final Map<String, String> INTERNAL_PLUGINS;
    private static final Set<String> DEFAULT_PLUGINS;

    private final BuildConfigWithSettings buildConfig;
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final TaskRegistry taskRegistry;
    private final Set<String> configuredPluginSettings = new CopyOnWriteArraySet<>();

    static {
        final Map<String, String> internalPlugins = new HashMap<>();
        internalPlugins.put("java", "jobt.plugin.java.JavaPlugin");
        internalPlugins.put("mavenresolver", "jobt.plugin.mavenresolver.MavenResolverPlugin");
        internalPlugins.put("checkstyle", "jobt.plugin.checkstyle.CheckstylePlugin");
        internalPlugins.put("findbugs", "jobt.plugin.findbugs.FindbugsPlugin");
        internalPlugins.put("idea", "jobt.plugin.idea.IdeaPlugin");
        internalPlugins.put("eclipse", "jobt.plugin.eclipse.EclipsePlugin");
        INTERNAL_PLUGINS = Collections.unmodifiableMap(internalPlugins);

        final Set<String> defaultPlugins = new HashSet<>();
        defaultPlugins.add("java");
        defaultPlugins.add("mavenresolver");
        DEFAULT_PLUGINS = Collections.unmodifiableSet(defaultPlugins);
    }

    public PluginRegistry(final BuildConfigWithSettings buildConfig,
                          final RuntimeConfigurationImpl runtimeConfiguration,
                          final TaskRegistry taskRegistry) {

        this.buildConfig = buildConfig;
        this.runtimeConfiguration = runtimeConfiguration;
        this.taskRegistry = taskRegistry;
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

        validateSettings();
    }

    private void initPlugin(final String plugin)
        throws Exception {

        LOG.info("Initialize plugin {}", plugin);
        final String pluginClassname = INTERNAL_PLUGINS.get(plugin);
        if (pluginClassname == null) {
            throw new IllegalArgumentException("Unknown plugin: " + plugin);
        }

        final URL pluginJarUrl = findPluginUrl(plugin);

        LOG.info("Load plugin {} using jar file from {}", plugin, pluginJarUrl);

        // Note that plugin dependencies are specified in MANIFEST.MF
        // @link https://docs.oracle.com/javase/tutorial/deployment/jar/downman.html

        final URLClassLoader classLoader = new URLClassLoader(
            new URL[] {pluginJarUrl},
            new BiSectFilteringClassLoader(
                getPlatformClassLoader(),
                Thread.currentThread().getContextClassLoader()
                ));

        final Class<?> aClass = classLoader.loadClass(pluginClassname);
        final Plugin regPlugin = (Plugin) aClass.newInstance();
        regPlugin.setTaskRegistry(taskRegistry);
        regPlugin.setBuildConfig(buildConfig);
        regPlugin.setRuntimeConfiguration(runtimeConfiguration);
        injectPluginSettings(plugin, regPlugin);
        regPlugin.configure();

        LOG.info("Plugin {} initialized", plugin);
    }

    private URL findPluginUrl(final String name) {
        final Path baseDir = Paths.get(System.getProperty("user.home"), ".jobt", "binary",
            Version.getVersion(), "plugin-" + name);
        return buildUrl(baseDir.resolve(
            String.format("plugin-%s-%s.jar", name, Version.getVersion())));
    }

    private static URL buildUrl(final Path f) {
        try {
            return f.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ClassLoader getPlatformClassLoader() {
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

    private void validateSettings() {
        final Set<String> unknownSettings = new HashSet<>(buildConfig.getSettings().keySet());
        unknownSettings.removeAll(configuredPluginSettings);

        if (!unknownSettings.isEmpty()) {
            throw new IllegalStateException("Unknown settings: " + unknownSettings);
        }
    }

}
