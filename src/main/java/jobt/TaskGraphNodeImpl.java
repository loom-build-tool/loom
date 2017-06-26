package jobt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jobt.api.ProductGraphNode;
import jobt.api.TaskGraphNode;
import jobt.util.Preconditions;

public class TaskGraphNodeImpl implements TaskGraphNode {

    private final String name;
    private final List<TaskGraphNode> dependentNodes = new ArrayList<>();
    private final List<ProductGraphNode> providedProductNodes = new ArrayList<>();
    private final List<ProductGraphNode> usedProductNodes = new ArrayList<>();

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

    @Override
    public void uses(final ProductGraphNode... products) {
        usedProductNodes.addAll(Arrays.asList(products));
    }

    public List<ProductGraphNode> getUsedProductNodes() {
        return usedProductNodes;
    }

    public void setProvidedProducts(final ProductGraphNode... products) {
        Preconditions.checkState(providedProductNodes.isEmpty());
        providedProductNodes.addAll(Arrays.asList(products));
    }

    public List<ProductGraphNode> getProvidedProductNodes() {
        return providedProductNodes;
    }

}
