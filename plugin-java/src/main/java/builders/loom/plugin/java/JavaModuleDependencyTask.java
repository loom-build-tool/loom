package builders.loom.plugin.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.TaskResult;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ModulePathProduct;
import builders.loom.api.product.ModulesPathProduct;

public class JavaModuleDependencyTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private GlobalProductRepository globalProductRepository;

    @Override
    public void setGlobalProductRepository(final GlobalProductRepository globalProductRepository) {
        this.globalProductRepository = globalProductRepository;
    }

    public JavaModuleDependencyTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public TaskResult run() throws Exception {

        final Set<String> moduleDependencies = buildConfig.getModuleDependencies();


        final List<ModulePathProduct> moduleProducts = new ArrayList<>();

        for (final String moduleName : moduleDependencies) {
            final CompilationProduct compilation = globalProductRepository.requireProduct(moduleName, "compilation", CompilationProduct.class);
            moduleProducts.add(new ModulePathProduct(moduleName, compilation.getClassesDir()));
        }


        return completeOk(new ModulesPathProduct(moduleProducts));
    }

}
