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

package builders.loom.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.BuildContext;
import builders.loom.api.Module;
import builders.loom.api.ModuleBuildConfigAware;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.ProductDependenciesAware;
import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.Task;
import builders.loom.api.TaskResult;
import builders.loom.api.TaskStatus;
import builders.loom.api.UsedProducts;
import builders.loom.core.plugin.ConfiguredTask;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final String name;
    private final BuildContext buildContext;
    private final RuntimeConfiguration runtimeConfiguration;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final ConfiguredTask configuredTask;
    private final ProductRepository productRepository;
    private final Map<Module, Set<Module>> transitiveModuleCompileDependencies;
    private UsedProducts usedProducts;
    private final Map<BuildContext, ProductRepository> moduleProductRepositories;
    private final Set<Module> modules;

    @SuppressWarnings("checkstyle:parameternumber")
    Job(final String name,
        final BuildContext buildContext,
        final RuntimeConfiguration runtimeConfiguration,
        final ConfiguredTask configuredTask,
        final ProductRepository productRepository,
        final Map<Module, Set<Module>> transitiveModuleCompileDependencies,
        final Map<BuildContext, ProductRepository> moduleProductRepositories) {

        this.name = Objects.requireNonNull(name, "name required");
        this.buildContext = buildContext;
        this.runtimeConfiguration = runtimeConfiguration;
        this.configuredTask = Objects.requireNonNull(configuredTask, "configuredTask required");
        this.productRepository =
            Objects.requireNonNull(productRepository, "productRepository required");
        this.transitiveModuleCompileDependencies = transitiveModuleCompileDependencies;
        this.moduleProductRepositories = moduleProductRepositories;
        this.modules = moduleProductRepositories.keySet().stream()
            .filter(Module.class::isInstance)
            .map(Module.class::cast).collect(Collectors.toSet());
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

        final ProductPromise productPromise = productRepository
            .lookup(configuredTask.getProvidedProduct());

        productPromise
            .setStartTime(System.nanoTime());

        final Supplier<Task> taskSupplier = configuredTask.getTaskSupplier();
        Thread.currentThread().setContextClassLoader(taskSupplier.getClass().getClassLoader());
        final Task task = taskSupplier.get();
        injectTaskProperties(task);

        final TaskResult taskResult = task.run();

        LOG.info("Task resulted with {}", taskResult);

        if (taskResult == null) {
            throw new IllegalStateException("Task <" + name + "> must not return null");
        }
        if (taskResult.getStatus() == null) {
            throw new IllegalStateException("Task <" + name + "> must not return null status");
        }
        if (taskResult.getProduct() == null && taskResult.getStatus() != TaskStatus.EMPTY) {
            throw new IllegalStateException("Task <" + name + "> returned null product with "
                + "status: " + taskResult.getStatus());
        }

        productPromise.complete(taskResult);

        return taskResult.getStatus();
    }

    private void injectTaskProperties(final Task task) {
        task.setRuntimeConfiguration(runtimeConfiguration);
        task.setBuildContext(buildContext);
        if (task instanceof ProductDependenciesAware) {
            final ProductDependenciesAware pdaTask = (ProductDependenciesAware) task;
            usedProducts = buildProductView();
            pdaTask.setUsedProducts(usedProducts);
        }
        if (task instanceof ModuleBuildConfigAware) {
            final ModuleBuildConfigAware mbcaTask = (ModuleBuildConfigAware) task;
            final Module module = (Module) buildContext;
            mbcaTask.setModuleBuildConfig(module.getConfig());
        }
        if (task instanceof ModuleGraphAware) {
            final ModuleGraphAware mgaTask = (ModuleGraphAware) task;
            mgaTask.setTransitiveModuleGraph(transitiveModuleCompileDependencies);
        }
    }

    private UsedProducts buildProductView() {

        final Stream<ProductPromise> usedProductsPromises =
            configuredTask.getUsedProducts().stream()
                .map(moduleProductRepositories.get(buildContext)::lookup);


        final Stream<ProductPromise> importedProductPromises = modules.stream()
            .flatMap(m -> m.getConfig().getModuleCompileDependencies().stream())
            .flatMap(moduleName -> configuredTask.getImportedProducts().stream()
                .map(p -> buildModuleProduct(moduleName, p)));

        final Stream<ProductPromise> importedAllProductPromises = modules.stream()
            .flatMap(bc -> configuredTask.getImportedAllProducts().stream()
                .map(p -> buildModuleProduct(bc.getModuleName(), p)));

        final Set<ProductPromise> productPromises =
            Stream.concat(
                usedProductsPromises,
                Stream.concat(
                    importedAllProductPromises,
                    importedProductPromises))
                .collect(Collectors.toSet());

        return new UsedProducts(buildContext.getModuleName(), productPromises);
    }

    private ProductPromise buildModuleProduct(final String moduleName, final String productId) {
        Objects.requireNonNull(moduleName, "moduleName required");
        Objects.requireNonNull(productId, "productId required");

        return moduleProductRepositories.entrySet().stream()
            .filter(e -> e.getKey().getModuleName().equals(moduleName))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Module <" + moduleName + "> not found"))
            .lookup(productId);
    }

    Optional<Set<ProductPromise>> getActuallyUsedProducts() {
        return Optional.ofNullable(usedProducts)
            .map(UsedProducts::getActuallyUsedProducts);
    }

    @Override
    public String toString() {
        return "Job{"
            + "name='" + name + '\''
            + ", status=" + status
            + '}';
    }

}
