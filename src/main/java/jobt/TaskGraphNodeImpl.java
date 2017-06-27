package jobt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jobt.api.ProductGraphNode;
import jobt.api.TaskGraphNode;
import jobt.util.Preconditions;

public class TaskGraphNodeImpl implements TaskGraphNode {

    private final String name;
    private final List<TaskGraphNode> dependentNodes = new ArrayList<>();
    private List<ProductGraphNode> providedProductNodes;
    private List<ProductGraphNode> usedProductNodes;

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
        return Collections.unmodifiableList(dependentNodes);
    }

    @Override
    public void uses(final ProductGraphNode... products) {
        Preconditions.checkState(
            usedProductNodes == null, "Cannot re-assign <usedProductNodes>");
        usedProductNodes = Arrays.asList(products);
    }

    public List<ProductGraphNode> getUsedProductNodes() {
        if (usedProductNodes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(usedProductNodes);
    }

    public void setProvidedProducts(final ProductGraphNode... products) {
        Preconditions.checkState(
            providedProductNodes == null, "Cannot re-assign <providedProductNodes>");
        providedProductNodes = Arrays.asList(products);
    }

    public List<ProductGraphNode> getProvidedProductNodes() {
        if (providedProductNodes == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(providedProductNodes);
    }

}
