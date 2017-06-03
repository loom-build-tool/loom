package jobt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jobt.api.TaskGraphNode;
import jobt.api.TaskTemplate;
import jobt.config.BuildConfigImpl;
import jobt.plugin.PluginRegistry;

@SuppressWarnings("checkstyle:regexpmultiline")
public class TaskTemplateImpl implements TaskTemplate {

    private static final Logger LOG = Logger.getLogger(TaskTemplateImpl.class.getName());

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

        LOG.info("Will execute "
            + resolvedTasks.stream().collect(Collectors.joining(" > ")));

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

        final AtomicInteger errors = new AtomicInteger();

        final Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                System.out.println("INC ERROR");
                errors.incrementAndGet();
            }
        };
        final ExecutorService fjp = new ForkJoinPool(jobs.size(),
            ForkJoinPool.defaultForkJoinWorkerThreadFactory, handler, false);

        fjp.invokeAll(jobs.values());

        LOG.info("Shutting down Job Pool");

        fjp.shutdown();

        LOG.info("Awaiting Job Pool shutdown");

        while (true) {
            fjp.awaitTermination(1, TimeUnit.SECONDS);
            System.out.println("Errors: " + errors.get());
            if (errors.get() > 0) {
                throw new IllegalStateException("Errors available");
            }
            if (fjp.isTerminated()) {
                break;
            }
        }

        LOG.info("Job Pool has shut down");

        // TODO handle failed

        tasksExecuted.addAll(resolvedTasks);
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
