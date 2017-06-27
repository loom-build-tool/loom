package jobt.api;

public interface TaskGraphNode {

    String getName();

    @Deprecated
    void dependsOn(TaskGraphNode... tasks);

    void uses(ProductGraphNode... products);

}
