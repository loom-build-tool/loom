package builders.loom.api;

public interface ProductRepository {

    ProductPromise lookup(String productId);

    void createProduct(String productId);

}
