package jobt.api;

public interface TaskGraphNode {

    String getName();

    void dependsOn(TaskGraphNode... tasks);

}