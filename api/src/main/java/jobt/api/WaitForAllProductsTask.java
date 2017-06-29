package jobt.api;

import java.util.Set;

import jobt.api.product.DummyProduct;

public class WaitForAllProductsTask extends AbstractTask {

    @Override
    public TaskStatus run() throws Exception {
        final ProvidedProducts providedProducts = getProvidedProducts();
        final Set<String> producedProductIds = providedProducts.getProducedProductIds();

        if (producedProductIds.size() != 1) {
            throw new IllegalStateException("WaitForAllProductsTask accepts only exact 1 product");
        }

        final UsedProducts usedProducts = getUsedProducts();
        for (final String productId : usedProducts.getAllowedProductIds()) {
            usedProducts.waitForProduct(productId);
        }

        final String productId = producedProductIds.iterator().next();
        providedProducts.complete(productId, new DummyProduct(productId));

        return TaskStatus.OK;
    }

}
