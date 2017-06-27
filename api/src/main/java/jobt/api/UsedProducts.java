package jobt.api;

import java.util.Collections;
import java.util.HashSet;
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

    private final ProductRepository productRepository;

    private final Set<String> allowedProductIds;

    public UsedProducts(
        final Set<String> allowedProductIds,
        final ProductRepository productRepository) {
        Objects.requireNonNull(allowedProductIds);
        allowedProductIds.forEach(id ->
            Preconditions.checkState(
                ProvidedProducts.PATTERN.matcher(id).matches(),
                "Invalid format of product id <%s>", id));
        Objects.requireNonNull(productRepository);
        this.allowedProductIds = Collections.unmodifiableSet(new HashSet<>(allowedProductIds));
        this.productRepository = productRepository;
    }

    public <T extends Product> T readProduct(final String productId, final Class<T> clazz) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(clazz);

        final long start = System.currentTimeMillis();

        final ProductPromise productPromise = productRepository.lookup(productId);

        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError(
                "Access to productId <" + productId + "> not configured for task");
        }

        final Object value = productPromise.getAndWaitForProduct();

        final long timeElapsed = System.currentTimeMillis() - start;
        LOG.debug("Blocked for {}ms waiting for product <{}>", timeElapsed, productId);

        return clazz.cast(value);
    }

    public void waitForProduct(final String productId) {

        final ProductPromise productPromise = productRepository.lookup(productId);

        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError(
                "Access to productId <" + productId + "> not configured for task");
        }

        productPromise.getAndWaitForProduct();
    }

    public Set<String> getAllowedProductIds() {
        return allowedProductIds;
    }

}
