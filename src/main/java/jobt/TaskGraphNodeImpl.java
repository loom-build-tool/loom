package jobt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jobt.api.TaskGraphNode;

public class TaskGraphNodeImpl implements TaskGraphNode {

    private final String name;
    private final List<TaskGraphNode> dependentNodes = new ArrayList<>();

    public TaskGraphNodeImpl(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void dependsOn(final TaskGraphNode... tasks) {
        dependentNodes.addAll(Arrays.asList(tasks));
    }

    public List<TaskGraphNode> getDependentNodes() {
        return dependentNodes;
    }

}
