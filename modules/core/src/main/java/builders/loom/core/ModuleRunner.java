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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.BuildContext;
import builders.loom.api.GlobalBuildContext;
import builders.loom.api.Module;
import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.core.misc.DirectedGraph;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.core.plugin.GoalInfo;
import builders.loom.core.plugin.PluginLoader;
import builders.loom.core.plugin.ProductRepositoryImpl;
import builders.loom.core.plugin.ServiceLocatorImpl;
import builders.loom.core.plugin.TaskInfo;
import builders.loom.core.plugin.TaskRegistryImpl;
import builders.loom.util.Stopwatch;

@SuppressWarnings({"checkstyle:classfanoutcomplexity", "checkstyle:classdataabstractioncoupling"})
public class ModuleRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleRunner.class);

    private static final long NANO_MILLI = 1_000_000;

    private final RuntimeConfiguration runtimeConfiguration;
    private final PluginLoader pluginLoader;
    private final ModuleRegistry moduleRegistry;
    private final ProgressMonitor progressMonitor;
    private final Map<BuildContext, TaskRegistryImpl> moduleTaskRegistries = new HashMap<>();
    private final Map<BuildContext, ServiceLocatorImpl> moduleServiceLocators = new HashMap<>();
    private final Map<BuildContext, ProductRepository> moduleProductRepositories = new HashMap<>();
    private final Map<Module, Set<Module>> transitiveModuleCompileDependencies = new HashMap<>();

    public ModuleRunner(final RuntimeConfiguration runtimeConfiguration,
                        final PluginLoader pluginLoader,
                        final ModuleRegistry moduleRegistry,
                        final ProgressMonitor progressMonitor) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.pluginLoader = pluginLoader;
        this.moduleRegistry = moduleRegistry;
        this.progressMonitor = progressMonitor;
    }

    public void init() {
        final Stopwatch sw = new Stopwatch();

        resolveModuleDependencyGraph();

        registerGlobalPlugins();
        registerModulePlugins();

        LOG.debug("Initialized all plugins in {}", sw);
    }

    private void registerGlobalPlugins() {
        LOG.info("Initialize Plugins for global build context");
        registerModule(Set.of("eclipse", "idea"),
            new GlobalBuildContext(runtimeConfiguration.getProjectBaseDir()));
    }

    private void registerModule(final Set<String> defaultPlugins, final BuildContext buildContext) {
        final TaskRegistryImpl taskRegistry = new TaskRegistryImpl(buildContext);
        final ServiceLocatorImpl serviceLocator = new ServiceLocatorImpl();

        final Set<String> pluginsToInitialize = new HashSet<>();
        pluginsToInitialize.addAll(defaultPlugins);
        pluginsToInitialize.addAll(buildContext.getConfig().getPlugins());

        pluginLoader.initPlugins(pluginsToInitialize, buildContext.getConfig(), taskRegistry,
            serviceLocator);

        moduleTaskRegistries.put(buildContext, taskRegistry);
        moduleServiceLocators.put(buildContext, serviceLocator);
        moduleProductRepositories.put(buildContext, new ProductRepositoryImpl());
    }

    private void registerModulePlugins() {
        final Set<String> defaultPlugins = Set.of("java", "maven");
        for (final Module module : moduleRegistry.getModules()) {
            LOG.info("Initialize Plugins for module {}", module.getModuleName());
            registerModule(defaultPlugins, module);
        }
    }

    public Optional<ExecutionReport> execute(final Set<String> productIds)
        throws BuildException, InterruptedException {

        final Stopwatch sw = new Stopwatch();

        final List<ConfiguredTask> resolvedTasks = resolveTasks(productIds).resolve(
            configuredTask -> productIds.contains(configuredTask.getProvidedProduct()));

        LOG.debug("Analyzed task dependency graph in {}", sw);

        if (resolvedTasks.isEmpty()) {
            return Optional.empty();
        }

        sw.reset();

        LOG.info("Execute {}", resolvedTasks.stream()
            .map(ConfiguredTask::toString)
            .collect(Collectors.joining(", ")));

        registerProducts();

        progressMonitor.setTasks(resolvedTasks.size());

        final Map<ConfiguredTask, Job> configuredTaskJobMap = new LinkedHashMap<>();
        resolvedTasks.forEach(ct -> configuredTaskJobMap.put(ct, buildJob(ct)));

        final JobPool jobPool = new JobPool(progressMonitor);
        jobPool.submitAll(configuredTaskJobMap.values());
        jobPool.shutdown();

        LOG.debug("Executed {} tasks in {}", resolvedTasks.size(), sw);

        final ExecutionReport executionReport = new ExecutionReport(resolvedTasks);

        resolvedTasks.stream()
            .sorted(Comparator.comparingLong(
                ct -> lookupProductPromise(
                    ct.getBuildContext(), ct.getProvidedProduct()).getCompletedAt()))
            .forEach(configuredTask -> {
                final ProductPromise productPromise =
                    lookupProductPromise(configuredTask.getBuildContext(),
                        configuredTask.getProvidedProduct());
                final Set<ProductPromise> actuallyUsedProducts =
                    configuredTaskJobMap.get(configuredTask)
                        .getActuallyUsedProducts().orElse(Collections.emptySet());

                final Optional<ProductPromise> latest = actuallyUsedProducts.stream()
                    .max(Comparator.comparingLong(ProductPromise::getCompletedAt));

                if (latest.isPresent()) {
                    executionReport.add(
                        configuredTask.toString(), configuredTask.getType(),
                        productPromise.getTaskStatus(),
                        productPromise.getCompletedAt() - latest.get().getCompletedAt());

                    LOG.info("Product {} was completed at {} after {}ms blocked by {} for {}ms",
                        productPromise.getProductId(), productPromise.getCompletedAt(),
                        (productPromise.getCompletedAt()
                            - productPromise.getStartTime()) / NANO_MILLI,
                        latest.get().getProductId(),
                        (productPromise.getCompletedAt()
                            - latest.get().getCompletedAt()) / NANO_MILLI);
                } else {
                    executionReport.add(configuredTask.toString(), configuredTask.getType(),
                        productPromise.getTaskStatus(),
                        productPromise.getCompletedAt()
                            - productPromise.getStartTime());

                    LOG.info("Product {} was completed at {} after {}ms without any dependencies",
                        productPromise.getProductId(), productPromise.getCompletedAt(),
                        (productPromise.getCompletedAt()
                            - productPromise.getStartTime()) / NANO_MILLI);
                }

            });

        return Optional.of(executionReport);
    }

    private void resolveModuleDependencyGraph() {
        final DirectedGraph<Module> dependentModules =
            new DirectedGraph<>(moduleRegistry.getModules());

        for (final Module module : moduleRegistry.getModules()) {
            final Set<Module> modCmpDeps = module.getConfig().getModuleCompileDependencies()
                .stream().map(m -> moduleRegistry.lookup(m)
                    .orElseThrow(() -> new IllegalStateException(
                        "Failed resolving dependent module "
                        + m + " from module " + module.getModuleName())))
                .collect(Collectors.toSet());

            dependentModules.addEdges(module, modCmpDeps);
        }

        for (final Module module : moduleRegistry.getModules()) {
            final Set<String> moduleDeps = module.getConfig().getModuleCompileDependencies();
            final List<Module> resolvedModCmpDeps =
                dependentModules.resolve((m) -> moduleDeps.contains(m.getModuleName()));

            transitiveModuleCompileDependencies.put(module, new HashSet<>(resolvedModCmpDeps));
        }
    }

    private DirectedGraph<ConfiguredTask> resolveTasks(final Set<String> productIds) {
        final Set<ConfiguredTask> allConfiguredTasks = moduleTaskRegistries.values().stream()
            .flatMap(task -> task.configuredTasks().stream())
            .collect(Collectors.toSet());

        validateRequestedProducts(productIds, allConfiguredTasks);

        // Initialize directed graph with all available ConfiguredTask instances
        final DirectedGraph<ConfiguredTask> diGraph = new DirectedGraph<>(allConfiguredTasks);

        allBuildContexts().forEach(
            buildContext ->  {
                final Collection<ConfiguredTask> moduleConfiguredTasks =
                    moduleTaskRegistries.get(buildContext).configuredTasks();

                for (final ConfiguredTask moduleConfiguredTask : moduleConfiguredTasks) {
                    diGraph.addEdges(moduleConfiguredTask,
                        collectUsedTasks(moduleConfiguredTasks, moduleConfiguredTask));

                    if (buildContext instanceof  Module) {
                        diGraph.addEdges(moduleConfiguredTask,
                            collectImportedTasks((Module) buildContext, moduleConfiguredTask));
                    }

                    if (buildContext instanceof GlobalBuildContext) {
                        diGraph.addEdges(moduleConfiguredTask,
                            collectImportAllTasks(allConfiguredTasks, moduleConfiguredTask));
                    }
                }
            });

        return diGraph;
    }

    private Stream<Module> allModules() {
        return moduleTaskRegistries.keySet().stream()
            .filter(Module.class::isInstance).map(Module.class::cast);
    }

    private Stream<BuildContext> allBuildContexts() {
        return moduleTaskRegistries.keySet().stream();
    }

    private ProductPromise lookupProductPromise(
        final BuildContext buildContext, final String productId) {
        Objects.requireNonNull(buildContext);
        Objects.requireNonNull(productId);
        return moduleProductRepositories.get(buildContext).lookup(productId);
    }

    // find tasks providing requested products (within same module)
    private List<ConfiguredTask> collectUsedTasks(
        final Collection<ConfiguredTask> moduleConfiguredTasks,
        final ConfiguredTask moduleConfiguredTask) {

        return moduleConfiguredTask.getUsedProducts().stream()
            .map(usedProductId -> findProvidingProduct(moduleConfiguredTasks, usedProductId))
            .collect(Collectors.toList());
    }

    // find tasks providing imported products from dependent modules
    private List<ConfiguredTask> collectImportedTasks(final Module module,
                                                      final ConfiguredTask moduleConfiguredTask) {
        final Set<String> modCmpDeps = module.getConfig().getModuleCompileDependencies();

        return moduleConfiguredTask.getImportedProducts().stream()
            .map(importedProductId -> allModules()
                .filter(m -> modCmpDeps.contains(m.getModuleName()))
                .map(
                    m -> {
                        final TaskRegistryImpl taskRegistry = moduleTaskRegistries.get(m);
                        if (taskRegistry == null) {
                            throw new IllegalStateException(
                                "Unknown module name <" + m.getModuleName() + ">"
                                    + " in module dependencies"
                            );
                        }
                        return taskRegistry;
                    }
                )
                .map(TaskRegistryImpl::configuredTasks)
                .map(tasks -> findProvidingProduct(tasks, importedProductId))
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    // find tasks providing imported products from all modules
    private List<ConfiguredTask> collectImportAllTasks(final Set<ConfiguredTask> allConfiguredTasks,
                                                       final ConfiguredTask moduleConfiguredTask) {

        return moduleConfiguredTask.getImportedAllProducts().stream()
            .map(productId -> allConfiguredTasks.stream()
                .filter(t -> t.getProvidedProduct().equals(productId))
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private ConfiguredTask findProvidingProduct(
        final Collection<ConfiguredTask> moduleConfiguredTasks, final String usedProductId) {

        final List<ConfiguredTask> providingTasks = moduleConfiguredTasks.stream()
            .filter(t -> t.getProvidedProduct().equals(usedProductId))
            .collect(Collectors.toList());

        if (providingTasks.size() != 1) {
            throw new IllegalStateException("Found " + providingTasks.size()
                + " products providing " + usedProductId + " expected exactly 1");
        }

        return providingTasks.get(0);
    }

    private void validateRequestedProducts(final Set<String> productIds,
                                           final Set<ConfiguredTask> allConfiguredTasks) {

        final Set<String> allProvidedProducts = allConfiguredTasks.stream()
            .map(ConfiguredTask::getProvidedProduct)
            .collect(Collectors.toSet());

        final List<String> unknownProducts = productIds.stream()
            .filter(productId -> !allProvidedProducts.contains(productId))
            .collect(Collectors.toList());

        if (!unknownProducts.isEmpty()) {
            throw new IllegalStateException("Unknown products: " + unknownProducts);
        }
    }

    private void registerProducts() {
        moduleProductRepositories
            .forEach((key, value) -> moduleTaskRegistries.get(key).configuredTasks()
                .forEach(ct -> value.createProduct(
                    ct.getBuildContext().getModuleName(), ct.getProvidedProduct())));
    }

    private Job buildJob(final ConfiguredTask configuredTask) {
        final BuildContext buildContext = configuredTask.getBuildContext();
        final ProductRepository productRepository = moduleProductRepositories.get(buildContext);
        final ServiceLocatorImpl serviceLocator = moduleServiceLocators.get(buildContext);

        final String jobName = buildContext.getModuleName() + " > " + configuredTask.getName();

        return new Job(jobName, buildContext, runtimeConfiguration, configuredTask,
            productRepository, serviceLocator, transitiveModuleCompileDependencies,
            moduleProductRepositories);
    }

    public ProductPromise lookupProduct(final BuildContext buildContext, final String productId) {
        return moduleProductRepositories.get(buildContext).lookup(productId);
    }

    public Set<String> getPluginNames() {
        return moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .flatMap(ct -> ct.getPluginNames().stream())
            .collect(Collectors.toSet());
    }

    public Set<TaskInfo> describePluginTasks(final String pluginName) {
        return moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ct -> !ct.isGoal())
            .filter(ct -> ct.getPluginName().equals(pluginName))
            .map(TaskInfo::new)
            .collect(Collectors.toSet());
    }

    public Set<GoalInfo> describeGoals() {
        final Set<GoalInfo> goalInfos = new HashSet<>();

        moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ConfiguredTask::isGoal)
            .collect(Collectors.groupingBy(ConfiguredTask::getName,
                Collectors.flatMapping((ct) -> ct.getUsedProducts().stream(), Collectors.toSet())))
            .forEach((name, usedProducts) -> goalInfos.add(new GoalInfo(name, usedProducts)));

        return goalInfos;
    }

    public Set<TaskInfo> describeTasks() {
        return moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ct -> !ct.isGoal())
            .map(TaskInfo::new)
            .collect(Collectors.toSet());
    }

}

