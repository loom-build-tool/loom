package jobt.api;

import java.util.Set;

public interface ProductRepository {

    ProductPromise lookup(String productId);

    void createProduct(String productId);

    Set<String> getProductNames();

}
