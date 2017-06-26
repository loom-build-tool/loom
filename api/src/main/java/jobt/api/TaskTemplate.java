package jobt.api;

public interface TaskTemplate {

    TaskGraphNode task(String name);

    ProductGraphNode product(String productId);

    TaskGraphNode virtualProduct(String productId);

}
