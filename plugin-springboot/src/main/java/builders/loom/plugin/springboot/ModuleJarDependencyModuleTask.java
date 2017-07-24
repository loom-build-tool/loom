package builders.loom.plugin.springboot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.ModuleJarProduct;
import builders.loom.api.product.ModulesJarProduct;

public class ModuleJarDependencyModuleTask extends AbstractModuleTask {

    private GlobalProductRepository globalProductRepository;

    @Override
    public void setGlobalProductRepository(final GlobalProductRepository globalProductRepository) {
        this.globalProductRepository = globalProductRepository;
    }

    public ModuleJarDependencyModuleTask() {
    }

    @Override
    public TaskResult run() throws Exception {
        final Set<String> moduleDependencies = getModuleConfig().getModuleDependencies();

        final List<ModuleJarProduct> moduleProducts = new ArrayList<>();

        for (final String moduleName : moduleDependencies) {
            final AssemblyProduct compilation =
                globalProductRepository.requireProduct(moduleName, "jar", AssemblyProduct.class);
            moduleProducts.add(new ModuleJarProduct(moduleName, compilation.getAssemblyFile()));
        }

        return completeOk(new ModulesJarProduct(moduleProducts));
    }

}
