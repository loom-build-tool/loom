package jobt.api;

public interface TaskGraphNode {

    void dependsOn(TaskGraphNode... tasks);

}
