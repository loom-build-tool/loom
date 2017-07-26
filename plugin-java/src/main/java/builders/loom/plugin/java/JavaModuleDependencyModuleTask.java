package builders.loom.plugin.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.TaskResult;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ModulePathProduct;
import builders.loom.api.product.ModulesPathProduct;

public class JavaModuleDependencyModuleTask extends AbstractModuleTask {

    public JavaModuleDependencyModuleTask() {
    }

    @Override
    public TaskResult run() throws Exception {
        final Set<String> moduleDependencies = getModuleConfig().getModuleDependencies();
        final List<ModulePathProduct> moduleProducts = new ArrayList<>();

        for (final String moduleName : moduleDependencies) {
            final CompilationProduct compilation = requireProduct(moduleName, "compilation", CompilationProduct.class);
            moduleProducts.add(new ModulePathProduct(moduleName, compilation.getClassesDir()));
        }

        return completeOk(new ModulesPathProduct(moduleProducts));
    }

}