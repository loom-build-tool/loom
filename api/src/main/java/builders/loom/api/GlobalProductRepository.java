package builders.loom.api;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import builders.loom.api.product.Product;

public class GlobalProductRepository {

    private final Map<BuildContext, ProductRepository> moduleProductRepositories;

    public GlobalProductRepository(final Map<BuildContext, ProductRepository> moduleProductRepositories) {
        this.moduleProductRepositories = moduleProductRepositories;
    }

    public <P extends Product> P requireProduct(final String moduleName, final String productId, final Class<P> productClass)
        throws InterruptedException {

        return useProduct(moduleName, productId, productClass)
            .orElseThrow(() -> new IllegalStateException("Requested product <"
                + productId + "> is not present"));
    }

    public <P extends Product> Optional<P> useProduct(final String moduleName,
                                                      final String productId,
                                                      final Class<P> productClass)
        throws InterruptedException {
        Objects.requireNonNull(moduleName, "moduleName required");
        Objects.requireNonNull(productId, "productId required");
        Objects.requireNonNull(productClass, "productClass required");

        final ProductRepository productRepository = moduleProductRepositories.entrySet().stream()
            .filter(e -> e.getKey().getModuleName().equals(moduleName))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Module <" + moduleName + "> not found"));

        return new UsedProducts(Collections.singleton(productId), productRepository).readProduct(productId, productClass);
    }

    public Set<Module> getAllModules() {
        Set<Module> collect = moduleProductRepositories.keySet().stream()
            .filter(m -> m instanceof Module)
            .map(Module.class::cast)
            .collect(Collectors.toSet());
        return Collections.unmodifiableSet(collect);
    }

    public Set<ProductPromise> getAllProductPromises() {
        return moduleProductRepositories.values().stream()
            .flatMap(pr -> pr.getAllProductPromises().stream())
            .collect(Collectors.toSet());
    }

}
