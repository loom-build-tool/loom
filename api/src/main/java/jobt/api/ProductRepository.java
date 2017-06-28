package jobt.api;

import java.util.Set;

public interface ProductRepository {

    Set<String> getProductIds();

    ProductPromise lookup(String productId);

    void createProduct(String productId);

}
