package builders.loom;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.ProductDependenciesAware;
import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;
import builders.loom.api.Task;
import builders.loom.api.TaskStatus;
import builders.loom.api.UsedProducts;
import builders.loom.api.ProvidedProducts;
import builders.loom.api.ServiceLocatorAware;
import builders.loom.api.service.ServiceLocator;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.util.Preconditions;
import builders.loom.util.Stopwatch;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final String name;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final ConfiguredTask configuredTask;
    private final ProductRepository productRepository;
    private final ServiceLocator serviceLocator;

    Job(final String name, final ConfiguredTask configuredTask,
        final ProductRepository productRepository,
        final ServiceLocator serviceLocator) {
        this.name = Objects.requireNonNull(name, "name required");
        this.configuredTask = Objects.requireNonNull(configuredTask, "configuredTask required");
        this.productRepository =
            Objects.requireNonNull(productRepository, "productRepository required");
        this.serviceLocator = serviceLocator;
    }

    public String getName() {
        return name;
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
        final Supplier<Task> taskSupplier = configuredTask.getTaskSupplier();
        Thread.currentThread().setContextClassLoader(taskSupplier.getClass().getClassLoader());
        final Task task = taskSupplier.get();
        injectTaskProperties(task);
        final TaskStatus taskStatus = task.run();
        Stopwatch.stopProcess();

        LOG.info("Task {} resulted with {}", name, taskStatus);

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
                new ProvidedProducts(
                    configuredTask.getProvidedProducts(), productRepository, name));
            pdaTask.setUsedProducts(
                new UsedProducts(configuredTask.getUsedProducts(), productRepository));
        }
        if (task instanceof ServiceLocatorAware) {
            final ServiceLocatorAware slaTask = (ServiceLocatorAware) task;
            slaTask.setServiceLocator(serviceLocator);
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
