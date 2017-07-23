/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.GlobalProductRepository;
import builders.loom.api.Module;
import builders.loom.api.ProductDependenciesAware;
import builders.loom.api.ProductRepository;
import builders.loom.api.ProvidedProduct;
import builders.loom.api.ServiceLocatorAware;
import builders.loom.api.Task;
import builders.loom.api.TaskResult;
import builders.loom.api.TaskStatus;
import builders.loom.api.UsedProducts;
import builders.loom.api.service.ServiceLocator;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.util.Stopwatches;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final Module module;
    private final GlobalProductRepository globalProductRepository;
    private final String name;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final ConfiguredTask configuredTask;
    private final ProductRepository productRepository;
    private final ServiceLocator serviceLocator;

    Job(final String name, final Module module, final ConfiguredTask configuredTask,
        final ProductRepository productRepository,
        final GlobalProductRepository globalProductRepository,
        final ServiceLocator serviceLocator) {

        this.module = module;
        this.globalProductRepository = globalProductRepository;
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
        try {
            return runTask();
        } finally {
            status.set(JobStatus.STOPPED);
        }
    }

    public TaskStatus runTask() throws Exception {
        LOG.info("Start task {}", name);

        if (!configuredTask.isGoal()) {
            Stopwatches.startProcess("Task " + name);
        }

        final Supplier<Task> taskSupplier = configuredTask.getTaskSupplier();
        Thread.currentThread().setContextClassLoader(taskSupplier.getClass().getClassLoader());
        final Task task = taskSupplier.get();
        injectTaskProperties(task);
        final TaskResult taskResult = task.run();

        if (!configuredTask.isGoal()) {
            Stopwatches.stopProcess();
        }

        LOG.info("Task resulted with {}", taskResult);

        if (taskResult == null) {
            throw new IllegalStateException("Task <" + name + "> must not return null");
        }
        if (taskResult.getStatus() == null) {
            throw new IllegalStateException("Task <" + name + "> must not return null status");
        }
        if (taskResult.getProduct() == null && taskResult.getStatus() != TaskStatus.SKIP) {
            throw new IllegalStateException("Task <" + name + "> returned null product with "
                + "status: " + taskResult.getStatus());
        }

        productRepository
            .lookup(configuredTask.getProvidedProduct())
            .complete(taskResult.getProduct());

        return taskResult.getStatus();
    }

    private void injectTaskProperties(final Task task) {
        task.setModule(module);
        if (task instanceof ProductDependenciesAware) {
            final ProductDependenciesAware pdaTask = (ProductDependenciesAware) task;
            pdaTask.setGlobalProductRepository(globalProductRepository);
            pdaTask.setProvidedProduct(
                new ProvidedProduct(
                    configuredTask.getProvidedProduct(), productRepository, name));
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
