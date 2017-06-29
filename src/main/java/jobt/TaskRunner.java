package jobt;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ProductRepository;
import jobt.plugin.ConfiguredTask;
import jobt.plugin.TaskRegistryLookup;

public class TaskRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private final TaskRegistryLookup taskRegistry;
    private final ProductRepository productRepository;

    public TaskRunner(final TaskRegistryLookup taskRegistry,
        final ProductRepository productRepository) {
        this.taskRegistry = taskRegistry;
        this.productRepository = productRepository;
    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    public void execute(final Set<String> productIds) throws InterruptedException {
        final Collection<String> resolvedTasks = resolveTasks(productIds);

        if (resolvedTasks.isEmpty()) {
            return;
        }

        System.out.println("Execute "
            + resolvedTasks.stream().collect(Collectors.joining(" > ")));

        final JobPool jobPool = new JobPool();
        jobPool.submitAll(buildJobs(resolvedTasks));
        jobPool.shutdown();
    }

    private List<String> resolveTasks(final Set<String> productIds) {
        // Map productId -> taskName
        final Map<String, String> producersMap = buildInvertedProducersMap();

        producersMap.keySet().forEach(productRepository::createProduct);

        final List<String> resolvedTasks = new LinkedList<>();
        final Queue<String> workingProductIds = new LinkedList<>(productIds);
        while (!workingProductIds.isEmpty()) {
            final String workingProductId = workingProductIds.remove();

            final String taskName = producersMap.get(workingProductId);

            if (taskName == null) {
                throw new IllegalStateException("No task found providing " + workingProductId);
            }

            final ConfiguredTask configuredTask =
                taskRegistry.lookupTask(taskName);

            resolvedTasks.remove(taskName);
            resolvedTasks.add(0, taskName);

            workingProductIds.addAll(configuredTask.getUsedProducts());
        }

        return resolvedTasks;
    }

    private Map<String, String> buildInvertedProducersMap() {
        final Map<String, String> producersMap = new HashMap<>();

        for (final String taskName : taskRegistry.getTaskNames()) {
            final ConfiguredTask configuredTask = taskRegistry.lookupTask(taskName);
            for (final String providedProduct : configuredTask.getProvidedProducts()) {
                final String oldTaskName = producersMap.putIfAbsent(providedProduct, taskName);
                if (oldTaskName != null) {
                    throw new IllegalStateException("Product " + providedProduct + " provided by "
                        + taskName + " but was already provided by " + oldTaskName);
                }
            }
        }

        return producersMap;
    }

    private Collection<Job> buildJobs(final Collection<String> resolvedTasks) {
        // LinkedHashMap to guaranty same order to support single thread execution
        final Map<String, Job> jobs = new LinkedHashMap<>();
        for (final String resolvedTask : resolvedTasks) {
            final ConfiguredTask task = taskRegistry.lookupTask(resolvedTask);
            jobs.put(resolvedTask, new Job(resolvedTask, task, productRepository));
        }

        return jobs.values();
    }

}
