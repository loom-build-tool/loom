package jobt;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ProductDependenciesAware;
import jobt.api.ProductPromise;
import jobt.api.ProductRepository;
import jobt.api.ProvidedProducts;
import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.api.UsedProducts;
import jobt.plugin.ConfiguredTask;
import jobt.util.Preconditions;
import jobt.util.Stopwatch;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final String name;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final ConfiguredTask configuredTask;
    private final ProductRepository productRepository;

    Job(final String name, final ConfiguredTask configuredTask,
        final ProductRepository productRepository) {
        this.name = Objects.requireNonNull(name, "name required");
        this.configuredTask = Objects.requireNonNull(configuredTask, "configuredTask required");
        this.productRepository =
            Objects.requireNonNull(productRepository, "productRepository required");
    }

    public String getName() {
        return name;
    }

    public JobStatus getStatus() {
        return status.get();
    }

    @Override
    public TaskStatus call() throws Exception {
        status.set(JobStatus.RUNNING);
        final TaskStatus taskStatus = runTask();
        status.set(JobStatus.STOPPED);

        return taskStatus;
    }

    public TaskStatus runTask() throws Exception {
        LOG.info("Start task {}", name);

        Stopwatch.startProcess("Task " + name);
        final Task task = configuredTask.getTaskSupplier().get();
        injectTaskProperties(task);
        final TaskStatus taskStatus = task.run();
        LOG.info("Task {} resulted with {}", name, taskStatus);
        Stopwatch.stopProcess();
        Objects.requireNonNull(taskStatus, "Task <" + name + "> must not return null");
        checkIfAllProductsCompleted();
        return taskStatus;
    }

    private void checkIfAllProductsCompleted() {

        final Set<String> uncompletedProduct =
            configuredTask.getProvidedProducts().stream()
            .map(productRepository::lookup)
            .filter(product -> !product.isCompleted())
            .map(ProductPromise::getProductId)
            .collect(Collectors.toSet());

        Preconditions.checkState(uncompletedProduct.isEmpty(),
            "task.run <%s> did not complete(provide) the following products: %s",
            name, uncompletedProduct);

    }

    private void injectTaskProperties(final Task task) {
        if (task instanceof ProductDependenciesAware) {
            final ProductDependenciesAware pdaTask = (ProductDependenciesAware) task;
            pdaTask.setProvidedProducts(
                new ProvidedProducts(configuredTask.getProvidedProducts(), productRepository));
            pdaTask.setUsedProducts(
                new UsedProducts(configuredTask.getUsedProducts(), productRepository));
        }
    }

    @Override
    public String toString() {
        return "Job{"
            + "name='" + name + '\''
            + ", status=" + status
            + '}';
    }

}
