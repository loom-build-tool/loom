package jobt.api;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only view to products.
 */
public class UsedProducts {

    private static final Logger LOG = LoggerFactory.getLogger(UsedProducts.class);

    private final ExecutionContext executionContext;

    private final Set<String> allowedProductIds;

    public UsedProducts(
        final Set<String> allowedProductIds,
        final ExecutionContext executionContext) {
        this.allowedProductIds = allowedProductIds;
        this.executionContext = executionContext;
    }

    public <T> T readProduct(final String productId, final Class<T> clazz) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(clazz);
        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError("Access to productId <"+productId+"> not configured for task");
        }

        final ProductPromise productPromise =
            Objects.requireNonNull(
                executionContext.getProducts().get(productId), "No product found by id <"+productId+">");

        LOG.debug("Requesting product <{}> ...", productId);
        final Object value = productPromise.get();

        LOG.debug("Return product <{}> with value: {}", productId, value);

        return clazz.cast(value);
    }

}
