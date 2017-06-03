package jobt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jobt.api.TaskGraphNode;
import jobt.api.TaskStatus;
import jobt.api.TaskTemplate;
import jobt.config.BuildConfigImpl;
import jobt.plugin.PluginRegistry;

@SuppressWarnings("checkstyle:regexpmultiline")
public class TaskTemplateImpl implements TaskTemplate {

    private final PluginRegistry pluginRegistry;
    private final Map<String, TaskGraphNodeImpl> tasks = new HashMap<>();
    private final List<String> tasksExecuted = new ArrayList<>();

    public TaskTemplateImpl(final BuildConfigImpl buildConfig, final Stopwatch stopwatch) {
        this.pluginRegistry = new PluginRegistry(buildConfig, this, stopwatch);
    }

    @Override
    public TaskGraphNode task(final String name) {
        final TaskGraphNode taskGraphNode = tasks.get(name);
        if (taskGraphNode != null) {
            return taskGraphNode;
        }

        final TaskGraphNodeImpl newTask = new TaskGraphNodeImpl(name);
        tasks.put(name, newTask);
        return newTask;
    }

    public void execute(final String task) throws Exception {
        final Set<String> resolvedTasks = resolveTasks(task);
        resolvedTasks.removeAll(tasksExecuted);

        if (resolvedTasks.isEmpty()) {
            return;
        }

        System.out.println("Will execute "
            + resolvedTasks.stream().collect(Collectors.joining(" > ")));

        for (final String resolvedTask : resolvedTasks) {
            final TaskStatus status = pluginRegistry.trigger(resolvedTask);

            if (status == TaskStatus.FAIL) {
                return;
            }
        }

        tasksExecuted.addAll(resolvedTasks);
    }

    private Set<String> resolveTasks(final String task) {
        final Set<String> resolvedTasks = new LinkedHashSet<>();
        resolveTasks(resolvedTasks, task);
        return resolvedTasks;
    }

    private void resolveTasks(final Set<String> resolvedTasks, final String taskName) {
        final TaskGraphNodeImpl taskGraphNode = tasks.get(taskName);
        for (final TaskGraphNode node : taskGraphNode.getDependentNodes()) {
            resolveTasks(resolvedTasks, ((TaskGraphNodeImpl) node).getName());
        }
        resolvedTasks.add(taskGraphNode.getName());
    }

}
