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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import builders.loom.api.TestProgressEmitter;
import builders.loom.api.TestProgressEmitterAware;
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
    private final Map<BuildContext, ProductRepository> moduleProductRepositories;
    private final Set<Module> modules;
    private final TestProgressEmitter testProgressEmitter;
    private UsedProducts usedProducts;

    @SuppressWarnings("checkstyle:parameternumber")
    Job(final String name,
        final BuildContext buildContext,
        final RuntimeConfiguration runtimeConfiguration,
        final ConfiguredTask configuredTask,
        final ProductRepository productRepository,
        final Map<Module, Set<Module>> transitiveModuleCompileDependencies,
        final Map<BuildContext, ProductRepository> moduleProductRepositories,
        final TestProgressEmitter emitter) {

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
        testProgressEmitter = emitter;
    }

    public String getName() {
        return name;
    }

    @Override
    public TaskStatus call() throws Exception {
        status.set(JobStatus.RUNNING);
        try {
            LOG.info("Start task {}", name);
            usedProducts = buildProductView();

            final AbstractTaskExecutionStrategy strategy = runtimeConfiguration.isCacheEnabled()
                ? new CacheableTaskRun(prepareProductPromise())
                : new TaskRun(prepareProductPromise());

            return strategy.run().getStatus();
        } finally {
            status.set(JobStatus.STOPPED);
        }
    }

    private ProductPromise prepareProductPromise() {
        final ProductPromise productPromise = productRepository
            .lookup(configuredTask.getProvidedProduct());

        productPromise.startTimer();

        return productPromise;
    }

    private UsedProducts buildProductView() {
        final Set<ProductPromise> productPromises = new HashSet<>();

        // inner module dependencies
        configuredTask.getUsedProducts().stream()
            .map(moduleProductRepositories.get(buildContext)::lookup)
            .forEach(productPromises::add);

        // explicit import from other modules
        final Set<String> importedProducts = configuredTask.getImportedProducts();
        if (!importedProducts.isEmpty()) {
            final Module module = (Module) this.buildContext;
            module.getConfig().getModuleCompileDependencies().stream()
                .flatMap(moduleName -> importedProducts.stream()
                    .map(p -> buildModuleProduct(moduleName, p)))
                .forEach(productPromises::add);
        }

        // import from all modules (e.g. for Eclipse / IntelliJ plugin)
        final Set<String> importedAllProducts = configuredTask.getImportedAllProducts();
        if (!importedAllProducts.isEmpty()) {
            modules.stream()
                .map(Module::getModuleName)
                .flatMap(moduleName -> importedAllProducts.stream()
                    .map(p -> buildModuleProduct(moduleName, p)))
                .forEach(productPromises::add);
        }

        return new UsedProducts(buildContext.getModuleName(), productPromises);
    }

    private void injectTaskProperties(final Task task) {
        task.setRuntimeConfiguration(runtimeConfiguration);
        task.setBuildContext(buildContext);
        if (task instanceof ProductDependenciesAware) {
            final ProductDependenciesAware pdaTask = (ProductDependenciesAware) task;
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
        if (task instanceof TestProgressEmitterAware) {
            final TestProgressEmitterAware tpea = (TestProgressEmitterAware) task;
            tpea.setTestProgressEmitter(testProgressEmitter);
        }
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

    Set<ProductPromise> getActuallyUsedProducts() {
        if (usedProducts == null) {
            return Collections.emptySet();
        }
        return usedProducts.getActuallyUsedProducts();
    }

    @Override
    public String toString() {
        return "Job{"
            + "name='" + name + '\''
            + ", status=" + status
            + '}';
    }

    private final class CacheableTaskRun extends TaskRun {

        private final ProductPromise productPromise;
        private final TaskExecutionPrediction tep;
        private final CachedProduct cachedProduct;

        CacheableTaskRun(final ProductPromise productPromise) {
            super(productPromise);
            this.productPromise = productPromise;
            tep = new TaskExecutionPrediction(runtimeConfiguration, configuredTask, usedProducts);
            cachedProduct = new CachedProduct(runtimeConfiguration, configuredTask);
        }

        @Override
        protected boolean canSkip() {
            return tep.canSkipTask() && cachedProduct.available();
        }

        @Override
        protected TaskResult doSkip() {
            final TaskResult taskResult = TaskResult.skip(cachedProduct.load());

            LOG.info("Task (skipped) resulted with {}", taskResult);

            // FIXME check result

            // note on fail status: product may contain details about the failure (reports)
            productPromise.complete(taskResult);
            return taskResult;
        }

        @Override
        protected void beginTransaction() {
            cachedProduct.prepare();
            tep.clearSignature();
        }

        @Override
        protected TaskResult doWork() throws Exception {
            return super.doWork();
        }

        @Override
        protected void commitTransaction(final TaskResult taskResult) {
            cachedProduct.persist(taskResult);
            tep.commitSignature();
        }

    }

    private class TaskRun extends AbstractTaskExecutionStrategy {

        private final ProductPromise productPromise;

        TaskRun(final ProductPromise productPromise) {
            this.productPromise = productPromise;
        }

        @Override
        protected boolean canSkip() {
            return false;
        }

        @Override
        protected TaskResult doSkip() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void beginTransaction() {
        }

        @Override
        protected TaskResult doWork() throws Exception {
            final Supplier<Task> taskSupplier = configuredTask.getTaskSupplier();
            Thread.currentThread().setContextClassLoader(taskSupplier.getClass().getClassLoader());
            final Task task = taskSupplier.get();
            injectTaskProperties(task);

            final TaskResult taskResult = task.run();

            LOG.info("Task resulted with {}", taskResult);

            if (taskResult == null) {
                throw new IllegalStateException("Task <" + name + "> must not return null");
            }

            // note on fail status: product may contain details about the failure (reports)
            productPromise.complete(taskResult);

            if (taskResult.getStatus() == TaskStatus.FAIL) {
                throw new IllegalStateException("Task <" + name + "> resulted in failure: "
                    + taskResult.getErrorReason());
            }
            return taskResult;
        }

        @Override
        protected void commitTransaction(final TaskResult taskResult) {
        }

    }

    private abstract static class AbstractTaskExecutionStrategy {

        protected abstract boolean canSkip();

        protected abstract TaskResult doSkip();

        protected abstract void beginTransaction();

        protected abstract TaskResult doWork() throws Exception;

        protected abstract void commitTransaction(final TaskResult taskResult);

        final TaskResult run() throws Exception {
            final TaskResult taskResult;

            if (canSkip()) {
                taskResult = doSkip();
            } else {
                beginTransaction();
                taskResult = doWork();
                commitTransaction(taskResult);
            }

            return taskResult;
        }

    }

}
