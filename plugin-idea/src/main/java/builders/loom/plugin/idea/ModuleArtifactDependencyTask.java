package builders.loom.plugin.idea;

import java.util.ArrayList;
import java.util.List;

import builders.loom.api.AbstractTask;
import builders.loom.api.GlobalProductRepository;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;

public class ModuleArtifactDependencyTask extends AbstractTask {

    private final String productId;
    private GlobalProductRepository globalProductRepository;

    @Override
    public void setGlobalProductRepository(final GlobalProductRepository globalProductRepository) {
        this.globalProductRepository = globalProductRepository;
    }

    public ModuleArtifactDependencyTask(final String productId) {
        this.productId = productId;
    }

    @Override
    public TaskResult run() throws Exception {
        final List<ArtifactProduct> products = new ArrayList<>();
        for (final String moduleName : globalProductRepository.getAllModuleNames()) {
            final ArtifactListProduct moduleProduct = globalProductRepository.requireProduct(
                moduleName, productId, ArtifactListProduct.class);

            products.addAll(moduleProduct.getArtifacts());
        }

        return completeOk(new ArtifactListProduct(products));
    }

}
