package jobt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ProductGraphNode;
import jobt.api.ProvidedProducts;
import jobt.api.Task;
import jobt.api.TaskGraphNode;
import jobt.api.TaskStatus;
import jobt.api.TaskTemplate;
import jobt.api.UsedProducts;
import jobt.config.BuildConfigImpl;
import jobt.plugin.PluginRegistry;
import jobt.util.Preconditions;

@SuppressWarnings({"checkstyle:regexpmultiline", "checkstyle:classdataabstractioncoupling"})
public class TaskTemplateImpl implements TaskTemplate {

    private static final Logger LOG = LoggerFactory.getLogger(TaskTemplateImpl.class);

    private final PluginRegistry pluginRegistry;
    private final Map<String, TaskGraphNodeImpl> tasks = new ConcurrentHashMap<>();
    private final Map<String, ProductGraphNodeImpl> taskProducts = new ConcurrentHashMap<>();

    public TaskTemplateImpl(final BuildConfigImpl buildConfig,
                            final RuntimeConfigurationImpl runtimeConfiguration) {
        this.pluginRegistry =
            new PluginRegistry(buildConfig, runtimeConfiguration, this);
    }

    @Override
    public TaskGraphNode task(final String name) {
        return tasks.computeIfAbsent(name, TaskGraphNodeImpl::new);
    }

    public Map<String, TaskGraphNodeImpl> getTasks() {
        return Collections.unmodifiableMap(tasks);
    }

    @Override
    public ProductGraphNode product(final String productId) {
        return Objects.requireNonNull(
            taskProducts.computeIfAbsent(productId, ProductGraphNodeImpl::new));
    }

    /**
     * Create a task with a fake product of same name - making it runnable.
     */
    @Override
    public TaskGraphNode virtualProduct(final String productId) {
        Preconditions.checkState(
            !tasks.containsKey(productId) || isVirtual(productId),
            "Connot use <%s> as virtual product id because a task with same name already exists"
            + " - you may want to make that task to a virtual product", productId);
        final TaskGraphNodeImpl task = (TaskGraphNodeImpl) task(productId);
        task.setProvidedProducts(product(productId));
        return task;
    }

    private boolean isVirtual(final String productId) {
        Objects.requireNonNull(productId);
        return tasks.containsKey(productId) && taskProducts.containsKey(productId);
    }

    public Set<String> getAvailableTaskNames() {
        return Collections.unmodifiableSet(tasks.keySet());
    }

    public Set<String> getAvailableProductIds() {
        return Collections.unmodifiableSet(taskProducts.keySet());
    }

    public void execute(final String[] productIds) throws Exception {

        final List<String> resolvedTasks = calcRequiredTasks(
            calcTasksForProducts(new HashSet<>(Arrays.asList(productIds))));

        if (resolvedTasks.isEmpty()) {
            return;
        }

        System.out.println("Execute "
            + resolvedTasks.stream().collect(Collectors.joining(" > ")));

        final JobPool jobPool = new JobPool();
        jobPool.submitAll(buildJobs(resolvedTasks));
        jobPool.shutdown();
    }

    private Collection<Job> buildJobs(final Collection<String> resolvedTasks) {
        // LinkedHashMap to guaranty same order to support single thread execution
        final Map<String, Job> jobs = new LinkedHashMap<>();
        for (final String resolvedTask : resolvedTasks) {
            final Optional<Task> task = pluginRegistry.getTask(resolvedTask);
            jobs.put(resolvedTask, new Job(resolvedTask, task.orElse(new DummyTask(resolvedTask))));
        }

        return jobs.values();
    }

    private List<String> calcRequiredTasks(final Set<String> requestedTasks) {
        final List<String> resolvedTasks = new LinkedList<>();
        final Set<String> products = new LinkedHashSet<>();

        final Map<String, String> producersMap = buildInvertedProducersMap();

        final Queue<String> working = new LinkedList<>(requestedTasks);

        while (!working.isEmpty()) {

            final String name = working.remove();

            if (!resolvedTasks.contains(name)) {
                resolvedTasks.add(0, name);
            }

            tasks.get(name).getUsedProductNodes().stream()
                .map(ProductGraphNode::getProductId)
                .forEach(products::add);

            tasks.get(name).getUsedProductNodes().stream()
                .map(ProductGraphNode::getProductId)
                .peek(productId -> Preconditions.checkState(
                    producersMap.containsKey(productId),
                    "Producer task not found for product <%s>", productId))
                .map(producersMap::get)
                .forEach(working::add);

        }

        LOG.info("Current build requires products: {}", products);
        LOG.info("calcRequiredTasks={}", resolvedTasks);

        return resolvedTasks;
    }

    private Set<String> calcTasksForProducts(final Set<String> requestedProducts) {

        final Map<String, String> producersMap = buildInvertedProducersMap();

        return
        requestedProducts.stream()
            .peek(productId -> Preconditions.checkState(producersMap.containsKey(productId),
                "No such product: %s (available products: %s)",
                productId, getAvailableProductIds()))
            .map(productId -> producersMap.get(productId))
            .collect(Collectors.toSet());

    }

    public Map<String, String> buildInvertedProducersMap() {
        // productId -> taskName (producer)
        final Map<String, String> producers = new HashMap<>();
        for (final Entry<String, TaskGraphNodeImpl> entry : tasks.entrySet()) {
            entry.getValue().getProvidedProductNodes().stream()
                .map(node -> node.getProductId())
                .forEach(productId -> producers.put(productId, entry.getKey()));
        }
        return producers;
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

        @Override
        public void setProvidedProducts(final ProvidedProducts providedProducts) {
            // do nothing
        }

        @Override
        public void setUsedProducts(final UsedProducts usedProducts) {
            // do nothing
        }

    }

}
