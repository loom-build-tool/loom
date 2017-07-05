package builders.loom.plugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;

public class ProductRepositoryImpl implements ProductRepository {

    private final Map<String, ProductPromise> products = new ConcurrentHashMap<>();

    @Override
    public ProductPromise lookup(final String productId) {
        return
            Objects.requireNonNull(
                products.get(productId), "No product found by id <" + productId + ">");
    }

    @Override
    public void createProduct(final String productId) {

        final ProductPromise oldValue =
            products.putIfAbsent(productId, new ProductPromise(productId));

        if (oldValue != null) {
            throw new IllegalStateException(
                "Product <" + productId + "> already registered");
        }

    }

}
