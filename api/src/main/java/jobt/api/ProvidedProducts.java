package jobt.api;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sink for provided products from upstream task.
 */
public class ProvidedProducts {

    private static final Logger LOG = LoggerFactory.getLogger(ProvidedProducts.class);

    private final ExecutionContext executionContext;

    private final Set<String> producedProductIds;

    public ProvidedProducts(
        final Set<String> producedProductIds,
        final ExecutionContext executionContext) {
        this.producedProductIds = producedProductIds;
        this.executionContext = executionContext;
        registerProducts();
    }

    private void registerProducts() {
        for (final String productId : producedProductIds) {
            if (executionContext.getProducts().containsKey(productId)) {
                throw new IllegalStateException(
                    "Product <"+productId+"> already registered");
            }

            executionContext.getProducts().put(productId, new ProductPromise(productId));

        }

    }

    public <T> void complete(final String productId, final T value) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(value);
        if (!producedProductIds.contains(productId)) {
            throw new IllegalAccessError("Not allowed to resolve productId <"+productId+">");
        }
        final ProductPromise productPromise = Objects.requireNonNull(executionContext.getProducts().get(productId));

        productPromise.complete(value);

        LOG.debug("Product promise <{}> completed with value: {}", productId, value);

    }

}
