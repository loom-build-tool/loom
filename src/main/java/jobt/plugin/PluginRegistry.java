package jobt.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.MavenResolver;
import jobt.Stopwatch;
import jobt.TaskTemplateImpl;
import jobt.api.Plugin;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.config.BuildConfigImpl;
import jobt.plugin.base.BasePlugin;
import jobt.plugin.checkstyle.CheckstylePlugin;
import jobt.plugin.java.JavaPlugin;

public class PluginRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistry.class);

    private final TaskRegistryImpl taskRegistry = new TaskRegistryImpl();
    private final Stopwatch stopwatch;

    public PluginRegistry(final BuildConfigImpl buildConfig, final TaskTemplateImpl taskTemplate,
                          final Stopwatch stopwatch) {
        this.stopwatch = stopwatch;

        final List<String> plugins = buildConfig.getPlugins();
        plugins.add("base");

        final ExecutionContextImpl executionContext = new ExecutionContextImpl();
        final MavenResolver dependencyResolver = new MavenResolver();

        for (final String plugin : plugins) {
            final Plugin regPlugin;
            switch (plugin) {
                case "base":
                    regPlugin = new BasePlugin();
                    break;
                case "java":
                    regPlugin = new JavaPlugin();
                    break;
                case "checkstyle":
                    regPlugin = new CheckstylePlugin();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown plugin: " + plugin);
            }

            regPlugin.setBuildConfig(buildConfig);
            regPlugin.setExecutionContext(executionContext);
            regPlugin.setDependencyResolver(dependencyResolver);
            regPlugin.configure(taskTemplate);
            regPlugin.configure(taskRegistry);
        }
    }

    public TaskStatus trigger(final String phase) throws Exception {
        final List<Task> tasks = taskRegistry.getTasks(phase);

        if (tasks.isEmpty()) {
            return TaskStatus.SKIP;
        }

        final String stopwatchProcess = "Task " + phase;
        stopwatch.startProcess(stopwatchProcess);

        final Set<TaskStatus> statuses = new HashSet<>();

        for (final Task task : tasks) {
            LOG.info("Start task {}", phase);
            final TaskStatus status = task.run();
            LOG.info("Task {} resulted with {}", phase, status);
            statuses.add(status);
        }

        final TaskStatus ret;
        if (statuses.size() == 1) {
            ret = statuses.iterator().next();
        } else {
            ret = statuses.stream().anyMatch(s -> s == TaskStatus.OK)
                ? TaskStatus.OK : TaskStatus.UP_TO_DATE;
        }

        stopwatch.stopProcess(stopwatchProcess);
        return ret;
    }

}
