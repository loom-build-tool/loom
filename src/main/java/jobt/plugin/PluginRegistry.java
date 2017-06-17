package jobt.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.RuntimeConfigurationImpl;
import jobt.Stopwatch;
import jobt.TaskTemplateImpl;
import jobt.Version;
import jobt.api.Plugin;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.config.BuildConfigImpl;

@SuppressWarnings({"checkstyle:classfanoutcomplexity", "checkstyle:illegalcatch"})
public class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    private final BuildConfigImpl buildConfig;
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final TaskTemplateImpl taskTemplate;
    private final ExecutionContextImpl executionContext = new ExecutionContextImpl();
    private final TaskRegistryImpl taskRegistry = new TaskRegistryImpl();

    public PluginRegistry(final BuildConfigImpl buildConfig,
                          final RuntimeConfigurationImpl runtimeConfiguration,
                          final TaskTemplateImpl taskTemplate) {

        this.buildConfig = buildConfig;
        this.runtimeConfiguration = runtimeConfiguration;
        this.taskTemplate = taskTemplate;

        initPlugins();
    }

    private void initPlugins() {
        final AtomicReference<Throwable> firstException = new AtomicReference<>();

        final Set<String> plugins = new HashSet<>(buildConfig.getPlugins());
        plugins.add("java");
        plugins.add("mavenresolver");

        final ExecutorService executorService = Executors.newWorkStealingPool();
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
    }

    private void initPlugin(final String plugin)
        throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        LOG.info("Initialize plugin {}", plugin);
        final String pluginClassname;
        switch (plugin) {
            case "java":
                pluginClassname = "jobt.plugin.java.JavaPlugin";
                break;
            case "mavenresolver":
                pluginClassname = "jobt.plugin.mavenresolver.MavenResolverPlugin";
                break;
            case "checkstyle":
                pluginClassname = "jobt.plugin.checkstyle.CheckstylePlugin";
                break;
            case "findbugs":
                pluginClassname = "jobt.plugin.findbugs.FindbugsPlugin";
                break;
            default:
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
        regPlugin.setBuildConfig(buildConfig);
        regPlugin.setRuntimeConfiguration(runtimeConfiguration);
        regPlugin.setExecutionContext(executionContext);
        regPlugin.configure(taskTemplate);
        regPlugin.configure(taskRegistry);


        LOG.info("Plugin {} initialized", plugin);
    }

    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader().getParent();
    }

    private URL findPluginUrl(final String name) throws IOException {
        final Path baseDir = Paths.get(System.getProperty("user.home"), ".jobt", "binary",
            Version.getVersion(), "plugin-" + name);
        return buildUrl(baseDir.resolve("plugin-" + name + ".jar"));
    }

    private static URL buildUrl(final Path f) {
        try {
            return f.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public TaskStatus trigger(final String taskName) throws Exception {
        final Optional<Task> task = taskRegistry.getTasks(taskName);

        if (!task.isPresent()) {
            LOG.debug("No task for {} registered", taskName);
            return TaskStatus.SKIP;
        }

        final String stopwatchProcess = "Task " + taskName;
        Stopwatch.startProcess(stopwatchProcess);

        final Set<TaskStatus> statuses = new HashSet<>();

        LOG.info("Start task {}", taskName);
        final TaskStatus status = task.get().run();
        LOG.info("Task {} resulted with {}", taskName, status);
        statuses.add(status);

        final TaskStatus ret;
        if (statuses.size() == 1) {
            ret = statuses.iterator().next();
        } else {
            ret = statuses.stream().anyMatch(s -> s == TaskStatus.OK)
                ? TaskStatus.OK : TaskStatus.UP_TO_DATE;
        }

        Stopwatch.stopProcess();
        return ret;
    }

    public void warmup(final String taskName) throws Exception {
        final Optional<Task> task = taskRegistry.getTasks(taskName);
        if (task.isPresent()) {
            task.get().prepare();
        }
    }

}
