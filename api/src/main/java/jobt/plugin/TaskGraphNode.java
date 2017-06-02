package jobt.plugin;

public interface TaskGraphNode {

    void dependsOn(TaskGraphNode... tasks);

}
