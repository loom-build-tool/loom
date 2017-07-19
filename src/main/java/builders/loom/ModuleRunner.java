package builders.loom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.Module;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.PluginRegistry;
import builders.loom.plugin.ProductRepositoryImpl;
import builders.loom.plugin.ServiceLocatorImpl;
import builders.loom.plugin.TaskRegistryImpl;
import builders.loom.util.Stopwatch;

public class ModuleRunner {

    private final ModuleRegistry moduleRegistry;
    private final BuildConfigWithSettings buildConfig;
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final Map<Module, TaskRegistryImpl> moduleTaskRegistries = new HashMap<>();
    private final Map<Module, ServiceLocatorImpl> moduleServiceLocators = new HashMap<>();
    private final Map<Module, ProductRepositoryImpl> moduleProductRepositories = new HashMap<>();

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

    }

    public Collection<ConfiguredTask> execute(final Set<String> productIds) throws BuildException, InterruptedException {

        Collection<ConfiguredTask> ret = new ArrayList<>();
        for (final Module module : moduleRegistry.getModules()) {
            final TaskRunner taskRunner = new TaskRunner(
                module, moduleTaskRegistries.get(module), moduleProductRepositories.get(module), moduleServiceLocators.get(module));

            ret.addAll(taskRunner.execute(productIds));
        }



        return ret;

    }

}
