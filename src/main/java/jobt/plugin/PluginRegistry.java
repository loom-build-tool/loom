package jobt.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import jobt.MavenResolver;
import jobt.TaskTemplateImpl;
import jobt.api.Plugin;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.config.BuildConfigImpl;
import jobt.plugin.base.BasePlugin;
import jobt.plugin.checkstyle.CheckstylePlugin;
import jobt.plugin.java.JavaPlugin;

public class PluginRegistry {

    private final TaskRegistryImpl taskRegistry = new TaskRegistryImpl();

    public PluginRegistry(final BuildConfigImpl buildConfig, final TaskTemplateImpl taskTemplate) {
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
            regPlugin.setLogger(Logger.getLogger(plugin));
            regPlugin.configure(taskTemplate);
            regPlugin.configure(taskRegistry);
        }
    }

    public TaskStatus trigger(final String phase) throws Exception {
        final Set<TaskStatus> statuses = new HashSet<>();

        for (final Task task : taskRegistry.getTasks(phase)) {
            final TaskStatus status = task.run();
            if (status == TaskStatus.FAIL) {
                return TaskStatus.FAIL;
            }
            statuses.add(status);
        }

        if (statuses.size() == 1) {
            return statuses.iterator().next();
        }

        return statuses.stream().anyMatch(s -> s == TaskStatus.OK)
            ? TaskStatus.OK : TaskStatus.UP_TO_DATE;
    }

}
