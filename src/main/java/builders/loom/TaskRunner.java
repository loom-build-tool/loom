package builders.loom;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.ProductRepository;
import builders.loom.plugin.TaskRegistryLookup;
import builders.loom.plugin.TaskUtil;
import builders.loom.api.service.ServiceLocator;
import builders.loom.plugin.ConfiguredTask;

public class TaskRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private final TaskRegistryLookup taskRegistry;
    private final ProductRepository productRepository;
    private final ServiceLocator serviceLocator;

    public TaskRunner(final TaskRegistryLookup taskRegistry,
        final ProductRepository productRepository,
        final ServiceLocator serviceLocator) {
        this.taskRegistry = taskRegistry;
        this.productRepository = productRepository;
        this.serviceLocator = serviceLocator;
    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    public void execute(final Set<String> productIds) throws InterruptedException {
        final Collection<String> resolvedTasks = resolveTasks(productIds);

        if (resolvedTasks.isEmpty()) {
            return;
        }

        LOG.info("Execute {}", resolvedTasks);

        ProgressMonitor.setTasks(resolvedTasks.size());

        final JobPool jobPool = new JobPool();
        jobPool.submitAll(buildJobs(resolvedTasks));
        jobPool.shutdown();
    }

    private List<String> resolveTasks(final Set<String> productIds) {
        // Map productId -> taskName
        final Map<String, String> producersMap =
            TaskUtil.buildInvertedProducersMap(taskRegistry);

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

        LOG.info("Registered products: {}", producersMap.keySet());
        LOG.info("Registered services: {}", serviceLocator.getServiceNames());

        return resolvedTasks;
    }

    private Collection<Job> buildJobs(final Collection<String> resolvedTasks) {
        // LinkedHashMap to guaranty same order to support single thread execution
        final Map<String, Job> jobs = new LinkedHashMap<>();
        for (final String resolvedTask : resolvedTasks) {
            final ConfiguredTask task = taskRegistry.lookupTask(resolvedTask);
            jobs.put(resolvedTask, new Job(resolvedTask, task, productRepository, serviceLocator));
        }

        return jobs.values();
    }

}
