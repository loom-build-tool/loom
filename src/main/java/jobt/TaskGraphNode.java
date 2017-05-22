package jobt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskGraphNode {

    private final String name;
    private final List<TaskGraphNode> dependentNodes = new ArrayList<>();

    public TaskGraphNode(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void dependsOn(final TaskGraphNode... tasks) {
        dependentNodes.addAll(Arrays.asList(tasks));
    }

    public List<TaskGraphNode> getDependentNodes() {
        return dependentNodes;
    }

}
