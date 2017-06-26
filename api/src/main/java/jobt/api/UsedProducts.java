package jobt.api;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.util.Preconditions;

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
        allowedProductIds.forEach(id ->
            Preconditions.checkState(
                ProvidedProducts.PATTERN.matcher(id).matches(),
                "Invalid format of product id <%s>", id));
        Objects.requireNonNull(allowedProductIds);
        Objects.requireNonNull(executionContext);
        this.allowedProductIds = allowedProductIds;
        this.executionContext = executionContext;
    }

    public <T extends Product> T readProduct(final String productId, final Class<T> clazz) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(clazz);

        final long start = System.currentTimeMillis();

        final ProductPromise productPromise =
            Objects.requireNonNull(
                executionContext.getProducts().get(productId), "No product found by id <"+productId+">");

        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError("Access to productId <"+productId+"> not configured for task");
        }

        final Object value = productPromise.getAndWaitForProduct();

        final long timeElapsed = System.currentTimeMillis() - start;
        if (timeElapsed < 3) {
            LOG.debug("Returned product <{}> without blocking", productId);
        } else {
            LOG.debug("Blocked for {}ms waiting for product <{}>", timeElapsed, productId);
        }

        return clazz.cast(value);
    }

}
