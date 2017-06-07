package jobt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.TaskGraphNode;
import jobt.api.TaskTemplate;
import jobt.config.BuildConfigImpl;
import jobt.plugin.PluginRegistry;

@SuppressWarnings("checkstyle:regexpmultiline")
public class TaskTemplateImpl implements TaskTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TaskTemplateImpl.class);

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

    @SuppressWarnings("checkstyle:illegalcatch")
    public void execute(final String task) throws Exception {
        final Set<String> resolvedTasks = resolveTasks(task);
        resolvedTasks.removeAll(tasksExecuted);

        if (resolvedTasks.isEmpty()) {
            return;
        }

        LOG.info("Will execute {}",
            resolvedTasks.stream().collect(Collectors.joining(" > ")));

        final Collection<Job> jobs = buildJobs(resolvedTasks);

        for (final String resolvedTask : resolvedTasks) {
            pluginRegistry.warmup(resolvedTask);
        }

        final AtomicReference<Exception> firstException = new AtomicReference<>();

        final ExecutorService executor = Executors.newWorkStealingPool();

        for (final Job job : jobs) {
            CompletableFuture.runAsync(() -> {
                try {
                    job.call();
                } catch (final Throwable e) {
                    firstException.compareAndSet(null, e);
                    if (!(e instanceof InterruptedException)) {
                        LOG.error(e.getMessage(), e);
                    }
                    executor.shutdownNow();
                }
            }, executor);

            if (executor.isShutdown()) {
                break;
            }
        }

        LOG.debug("Shutting down Job Pool");
        executor.shutdown();

        LOG.debug("Awaiting Job Pool shutdown");
        executor.awaitTermination(1, TimeUnit.HOURS);
        LOG.debug("Job Pool has shut down");

        if (firstException.get() != null) {
            throw firstException.get();
        }

        tasksExecuted.addAll(resolvedTasks);
    }

    private Collection<Job> buildJobs(final Set<String> resolvedTasks) {
        final Map<String, Job> jobs = new HashMap<>();
        for (final String resolvedTask : resolvedTasks) {
            jobs.put(resolvedTask, new Job(resolvedTask,
                () -> pluginRegistry.trigger(resolvedTask)));
        }
        for (final Map.Entry<String, Job> stringJobEntry : jobs.entrySet()) {
            final TaskGraphNodeImpl taskGraphNode = tasks.get(stringJobEntry.getKey());
            final List<Job> dependentJobs = taskGraphNode.getDependentNodes().stream()
                .map(TaskGraphNode::getName)
                .map(jobs::get)
                .collect(Collectors.toList());
            stringJobEntry.getValue().setDependencies(dependentJobs);
        }

        // guaranty same order to support single thread execution
        return resolvedTasks.stream()
            .map(jobs::get)
            .collect(Collectors.toList());
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

}
