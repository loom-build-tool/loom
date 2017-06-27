package jobt.api;

public interface TaskGraphNode {

    String getName();

    void uses(ProductGraphNode... products);

}
