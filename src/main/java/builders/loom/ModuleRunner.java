package builders.loom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.Module;
import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.PluginRegistry;
import builders.loom.plugin.ProductRepositoryImpl;
import builders.loom.plugin.ServiceLocatorImpl;
import builders.loom.plugin.TaskInfo;
import builders.loom.plugin.TaskRegistryImpl;
import builders.loom.util.DirectedGraph;
import builders.loom.util.Stopwatch;

public class ModuleRunner {

	 private static final Logger LOG = LoggerFactory.getLogger(ModuleRunner.class);
	 
    private final ModuleRegistry moduleRegistry;
    private final BuildConfigWithSettings buildConfig;
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final Map<Module, TaskRegistryImpl> moduleTaskRegistries = new HashMap<>();
    private final Map<Module, ServiceLocatorImpl> moduleServiceLocators = new HashMap<>();
    private final Map<Module, ProductRepository> moduleProductRepositories = new HashMap<>();
    private GlobalProductRepository globalProductRepository;

    public ModuleRunner(final ModuleRegistry moduleRegistry, final BuildConfigWithSettings buildConfig, final RuntimeConfigurationImpl runtimeConfiguration) {

        this.moduleRegistry = moduleRegistry;
        this.buildConfig = buildConfig;
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public void init() {

        for (final Module module : moduleRegistry.getModules()) {
            Stopwatch.startProcess("Initialize plugin " + module.getModuleName());
            final ServiceLocatorImpl serviceLocator = new ServiceLocatorImpl();
            final TaskRegistryImpl taskRegistry = new TaskRegistryImpl();
            new PluginRegistry(module.getConfig(), runtimeConfiguration,
                taskRegistry, serviceLocator).initPlugins();

            moduleTaskRegistries.put(module, taskRegistry);
            moduleServiceLocators.put(module, serviceLocator);
            moduleProductRepositories.put(module, new ProductRepositoryImpl());
            Stopwatch.stopProcess();
        }

        globalProductRepository = new GlobalProductRepository(moduleProductRepositories);

    }
    
    static class ConfiguredModuleTask {
    		private final Module module;
		private final ConfiguredTask configuredTask;

			public ConfiguredModuleTask(
					final Module module, final ConfiguredTask configuredTask) {
				this.module = module;
				this.configuredTask = configuredTask;
			}
			
			public Module getModule() {
				return module;
			}

			public ConfiguredTask getConfiguredTask() {
				return configuredTask;
			}

			@Override
			public String toString() {
				return module.getModuleName() + "::" + configuredTask.getName();
			}
    }

    public List<ConfiguredModuleTask> execute(final Set<String> productIds) throws BuildException, InterruptedException {
    	
    	final DirectedGraph<ConfiguredModuleTask> diGraph = new DirectedGraph<>();
    	
    	for (final Module module : moduleRegistry.getModules()) {
    		final Collection<ConfiguredTask> configuredTasks = moduleTaskRegistries.get(module).configuredTasks();
    		
    		configuredTasks.stream().map(ct -> new ConfiguredModuleTask(module, ct))
    			.forEach(cmt -> diGraph.addNode(cmt));
    		
    	}
    	
    	for (final Module module : moduleRegistry.getModules()) {
    		final Collection<ConfiguredTask> configuredTasks = moduleTaskRegistries.get(module).configuredTasks();

    		for (final ConfiguredTask configuredTask : configuredTasks) {
    			for (final String productId : configuredTask.getUsedProducts()) {
    				diGraph.addEdge(
    						diGraph.nodes().stream().filter(cmt -> cmt.getConfiguredTask() == configuredTask).findFirst().get(),
    						diGraph.nodes().stream().filter(cmt -> cmt.getModule() == module && cmt.getConfiguredTask().getProvidedProduct().equals(productId)).findFirst().get()
    						);
    			}

    			for (final String productId : configuredTask.getImportedProducts()) {
    				for(final String depModuleName : module.getConfig().getModuleDependencies()) {
    					final Module depModule = diGraph.nodes().stream()
    							.map(cmt -> cmt.getModule())
    							.filter(m -> m.getModuleName().equals(depModuleName)).findFirst()
    							.orElseThrow(() -> new IllegalStateException("Dependent module " + depModuleName + " not found"));

    					diGraph.addEdge(
    							diGraph.nodes().stream().filter(cmt -> cmt.getConfiguredTask() == configuredTask).findFirst().get(),
    							diGraph.nodes().stream().filter(cmt -> cmt.getModule() == depModule && cmt.getConfiguredTask().getProvidedProduct().equals(productId)).findFirst().get()
    							);
    				}
    			}

    		}
    		
    	}

    	final List<ConfiguredModuleTask> explicitlyRequestedTasks = productIds.stream()
    		.flatMap(p -> diGraph.nodes().stream().filter(cmt -> cmt.getConfiguredTask().getProvidedProduct().equals(p)))
		.collect(Collectors.toList());
    	
     final List<ConfiguredModuleTask> resolvedTasks = diGraph.resolve(explicitlyRequestedTasks);
    		
//    	for (final Module module : moduleRegistry.getModules()) {
//    		System.out.println("calc tasks for module " + module.getModuleName());
//    		
//    		
//    		final Collection<ConfiguredTask> configuredTasks = moduleTaskRegistries.get(module).configuredTasks();
//    		
//    		final List<ConfiguredTask> tasks = configuredTasks.stream().filter(ct -> productIds.contains(ct.getProvidedProduct())).collect(Collectors.toList());
//    		tasks.stream().map(t -> t.getPluginName() + " " + t.getName()).forEach(str -> System.out.println(" - " + str));
//    		
//    		tasks.stream().flatMap(t -> t.getImportedProducts().stream())
//    		.map(p -> module.getModuleName() + "::" + p);
//    		
//    	}
//    		
//
     

     if (resolvedTasks.isEmpty()) {
         return Collections.emptyList();
     }

     LOG.info("Execute {}", resolvedTasks.stream()
         .map(ConfiguredModuleTask::toString)
         .collect(Collectors.joining(", ")));

     for(final Module module : moduleRegistry.getModules()) {
    	 	registerProducts(moduleProductRepositories.get(module), 
    	 			resolvedTasks.stream().filter(cmt -> cmt.getModule()== module).map(m -> m.getConfiguredTask()).collect(Collectors.toList()));
     }

     ProgressMonitor.setTasks(resolvedTasks.size());

     final JobPool jobPool = new JobPool();
     jobPool.submitAll(buildJobs(resolvedTasks));
     jobPool.shutdown();
     
     
        final Collection<ConfiguredTask> ret = new ArrayList<>();
//        for (final Module module : moduleRegistry.getModules()) {
//            final TaskRunner taskRunner = new TaskRunner(
//                module, globalProductRepository, moduleTaskRegistries.get(module), moduleProductRepositories.get(module), moduleServiceLocators.get(module));
//
//            ret.addAll(taskRunner.execute(productIds));
//        }



        return resolvedTasks;

    }
    

    private void registerProducts(final ProductRepository productRepository, final Collection<ConfiguredTask> resolvedTasks) {
        resolvedTasks.stream()
            .map(ConfiguredTask::getProvidedProduct)
            .forEach(productRepository::createProduct);
    }

    private Collection<Job> buildJobs(final Collection<ConfiguredModuleTask> resolvedTasks) {
        return resolvedTasks.stream()
            .map(cmt -> new Job(cmt.getModule(), globalProductRepository, 
            		cmt.getModule().getModuleName()+"::"+cmt.getConfiguredTask().getName(), cmt.getConfiguredTask(), moduleProductRepositories.get(cmt.getModule()), moduleServiceLocators.get(cmt.getModule())))
            .collect(Collectors.toList());
    }

	public ProductPromise lookupProduct(final Module module, final String productId) {
		return moduleProductRepositories.get(module).lookup(productId);
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

	public Set<TaskInfo> configuredTasks() {
		return moduleTaskRegistries.values().stream()
			.flatMap(reg -> reg.configuredTasks().stream())
			.map(TaskInfo::new)
			.collect(Collectors.toSet());
	}

}

