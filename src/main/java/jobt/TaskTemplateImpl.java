package jobt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.Task;
import jobt.api.TaskGraphNode;
import jobt.api.TaskStatus;
import jobt.api.TaskTemplate;
import jobt.config.BuildConfigImpl;
import jobt.plugin.PluginRegistry;

@SuppressWarnings("checkstyle:regexpmultiline")
public class TaskTemplateImpl implements TaskTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TaskTemplateImpl.class);

    private final PluginRegistry pluginRegistry;
    private final Map<String, TaskGraphNodeImpl> tasks = new ConcurrentHashMap<>();
    private final List<String> tasksExecuted = new ArrayList<>();

    public TaskTemplateImpl(final BuildConfigImpl buildConfig,
                            final RuntimeConfigurationImpl runtimeConfiguration) {
        this.pluginRegistry =
            new PluginRegistry(buildConfig, runtimeConfiguration, this);
    }

    @Override
    public TaskGraphNode task(final String name) {
        return tasks.computeIfAbsent(name, TaskGraphNodeImpl::new);
    }

    public void execute(final String task) throws Exception {
        final Set<String> resolvedTasks = resolveTasks(task);
        resolvedTasks.removeAll(tasksExecuted);

        if (resolvedTasks.isEmpty()) {
            return;
        }

        LOG.info("Will execute {}",
            resolvedTasks.stream().collect(Collectors.joining(" > ")));

        final Collection<Job> jobs = buildJobs(resolvedTasks);
        final JobPool jobPool = new JobPool();

        jobPool.submitAll(jobs);

        jobPool.shutdown();

        tasksExecuted.addAll(resolvedTasks);
    }

    private Collection<Job> buildJobs(final Set<String> resolvedTasks) {
        // LinkedHashMap to guaranty same order to support single thread execution
        final Map<String, Job> jobs = new LinkedHashMap<>();
        for (final String resolvedTask : resolvedTasks) {
            final Optional<Task> task = pluginRegistry.getTask(resolvedTask);
            jobs.put(resolvedTask, new Job(resolvedTask, task.orElse(new DummyTask(resolvedTask))));
        }
        for (final Map.Entry<String, Job> stringJobEntry : jobs.entrySet()) {
            final TaskGraphNodeImpl taskGraphNode = tasks.get(stringJobEntry.getKey());
            final List<Job> dependentJobs = taskGraphNode.getDependentNodes().stream()
                .map(TaskGraphNode::getName)
                .map(jobs::get)
                .collect(Collectors.toList());
            stringJobEntry.getValue().setDependencies(dependentJobs);
        }

        return jobs.values();
    }

    private Set<String> resolveTasks(final String task) {
        final Set<String> resolvedTasks = new LinkedHashSet<>();
        resolveTasks(resolvedTasks, task);
        return resolvedTasks;
    }

    private void resolveTasks(final Set<String> resolvedTasks, final String taskName) {
        final TaskGraphNodeImpl taskGraphNode = tasks.get(taskName);
        if (taskGraphNode == null) {
            throw new IllegalArgumentException("Unknown task " + taskName);
        }
        for (final TaskGraphNode node : taskGraphNode.getDependentNodes()) {
            resolveTasks(resolvedTasks, node.getName());
        }
        resolvedTasks.add(taskGraphNode.getName());
    }

    private static class DummyTask implements Task {

        private static final Logger LOG = LoggerFactory.getLogger(DummyTask.class);
        private final String taskName;

        DummyTask(final String taskName) {
            this.taskName = taskName;
        }

        @Override
        public void prepare() throws Exception {
            LOG.debug("Nothing to prepare for {}", taskName);
        }

        @Override
        public TaskStatus run() throws Exception {
            LOG.debug("Nothing to run for {}", taskName);
            return TaskStatus.OK;
        }

    }

}
