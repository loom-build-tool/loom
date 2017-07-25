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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.BuildContext;
import builders.loom.api.GlobalBuildContext;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.Module;
import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.GoalInfo;
import builders.loom.plugin.PluginLoader;
import builders.loom.plugin.ProductRepositoryImpl;
import builders.loom.plugin.ServiceLocatorImpl;
import builders.loom.plugin.TaskInfo;
import builders.loom.plugin.TaskRegistryImpl;
import builders.loom.util.DirectedGraph;
import builders.loom.util.Stopwatch;

public class ModuleRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleRunner.class);

    private final PluginLoader pluginLoader;
    private final ModuleRegistry moduleRegistry;
    private final Map<BuildContext, TaskRegistryImpl> moduleTaskRegistries = new HashMap<>();
    private final Map<BuildContext, ServiceLocatorImpl> moduleServiceLocators = new HashMap<>();
    private final Map<BuildContext, ProductRepository> moduleProductRepositories = new HashMap<>();
    private final Map<Module, Set<Module>> transitiveModuleDependencies = new HashMap<>();
    private GlobalProductRepository globalProductRepository;

    public ModuleRunner(final PluginLoader pluginLoader,
                        final ModuleRegistry moduleRegistry) {
        this.pluginLoader = pluginLoader;
        this.moduleRegistry = moduleRegistry;
    }

    public void init() {
        final Stopwatch sw = new Stopwatch();

        resolveModuleDependencyGraph();

        registerModule(Set.of("eclipse", "idea"), new GlobalBuildContext());

        final Set<String> defaultPlugins = Set.of("java", "mavenresolver");
        for (final Module module : moduleRegistry.getModules()) {
            LOG.info("Initialize Plugins for module {}", module.getModuleName());
            registerModule(defaultPlugins, module);
        }

        globalProductRepository = new GlobalProductRepository(moduleProductRepositories);

        LOG.debug("Initialized all plugins in {} ms", sw.duration());
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

    public List<ConfiguredTask> execute(final Set<String> productIds)
        throws BuildException, InterruptedException {

        final List<ConfiguredTask> resolvedTasks = resolveTasks(productIds);

            if (resolvedTasks.isEmpty()) {
                return Collections.emptyList();
            }

            final Stopwatch sw = new Stopwatch();

            LOG.info("Execute {}", resolvedTasks.stream()
                .map(ConfiguredTask::toString)
                .collect(Collectors.joining(", ")));

            registerProducts();

            ProgressMonitor.setTasks(resolvedTasks.size());

            final JobPool jobPool = new JobPool();
            jobPool.submitAll(resolvedTasks.stream().map(this::buildJob));
            jobPool.shutdown();

            LOG.debug("Executed {} tasks in {} ms", resolvedTasks.size(), sw.duration());

            return resolvedTasks;
        }

    private void resolveModuleDependencyGraph() {
        final DirectedGraph<Module> dependentModules = new DirectedGraph<>(moduleRegistry.getModules());

        for (final Module module : moduleRegistry.getModules()) {
            final Set<Module> moduleDependencies = module.getConfig().getModuleDependencies()
                .stream().map(m -> moduleRegistry.lookup(m)
                    .orElseThrow(() -> new IllegalStateException("Failed resolving dependent module "
                        + m + " from module " + module.getModuleName())))
                .collect(Collectors.toSet());

            dependentModules.addEdges(module, moduleDependencies);
        }

        for (final Module module : moduleRegistry.getModules()) {
            final Set<String> moduleDeps = module.getConfig().getModuleDependencies();
            final List<Module> resolve = dependentModules.resolve((m) -> moduleDeps.contains(m.getModuleName()));

            transitiveModuleDependencies.put(module, new HashSet<>(resolve));
        }
    }

    private List<ConfiguredTask> resolveTasks(final Set<String> productIds) {
        final Stopwatch sw = new Stopwatch();

        final Set<ConfiguredTask> allConfiguredTasks = moduleTaskRegistries.values().stream()
            .flatMap(task -> task.configuredTasks().stream())
            .collect(Collectors.toSet());

        validateRequestedProducts(productIds, allConfiguredTasks);

        // Initialize directed graph with all available ConfiguredTask instances
        final DirectedGraph<ConfiguredTask> diGraph = new DirectedGraph<>(allConfiguredTasks);

        for (final BuildContext buildContext : moduleTaskRegistries.keySet()) {
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
        }

        final List<ConfiguredTask> resolvedTasks = diGraph.resolve(
            configuredTask -> productIds.contains(configuredTask.getProvidedProduct()));

        LOG.debug("Analyzed task dependency graph in {} ms", sw.duration());
        return resolvedTasks;
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
        final Set<String> moduleDependencies = module.getConfig().getModuleDependencies();

        return moduleConfiguredTask.getImportedProducts().stream()
            .map(importedProductId -> moduleTaskRegistries.keySet().stream()
                .filter(m -> moduleDependencies.contains(m.getModuleName()))
                .map(m -> moduleTaskRegistries.get(m).configuredTasks()) // TODO throw module doesn't exist exception!!
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
                .forEach(ct -> value.createProduct(ct.getProvidedProduct())));
    }

    private Job buildJob(final ConfiguredTask configuredTask) {
        final BuildContext buildContext = configuredTask.getBuildContext();
        final ProductRepository productRepository = moduleProductRepositories.get(buildContext);
        final ServiceLocatorImpl serviceLocator = moduleServiceLocators.get(buildContext);

        final String jobName = buildContext.getModuleName() + " > " + configuredTask.getName();

        return new Job(jobName, buildContext, configuredTask, productRepository,
            globalProductRepository, serviceLocator, transitiveModuleDependencies);
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

    public Set<TaskInfo> configuredTasksByPluginName(final String pluginName) {
        return moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ct -> !ct.isGoal())
            .filter(ct -> ct.getPluginName().equals(pluginName))
            .map(TaskInfo::new)
            .collect(Collectors.toSet());
    }

    public Set<GoalInfo> configuredGoals() {
        final Set<GoalInfo> goalInfos = new HashSet<>();

        moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ConfiguredTask::isGoal)
            .collect(Collectors.groupingBy(ConfiguredTask::getName,
                Collectors.flatMapping((ct) -> ct.getUsedProducts().stream(), Collectors.toSet())))
            .forEach((name, usedProducts) -> goalInfos.add(new GoalInfo(name, usedProducts)));

        return goalInfos;
    }

    public Set<TaskInfo> configuredTasks() {
        return moduleTaskRegistries.values().stream()
            .flatMap(reg -> reg.configuredTasks().stream())
            .filter(ct -> !ct.isGoal())
            .map(TaskInfo::new)
            .collect(Collectors.toSet());
    }

}

