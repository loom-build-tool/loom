package jobt;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jobt.api.ProductGraphNode;
import jobt.api.TaskGraphNode;

public class TaskGraphNodeImpl implements TaskGraphNode {

    private final String name;
    private final List<ProductGraphNode> providedProductNodes = new CopyOnWriteArrayList<>();
    private final List<ProductGraphNode> usedProductNodes = new CopyOnWriteArrayList<>();

    public TaskGraphNodeImpl(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void uses(final ProductGraphNode... products) {
        usedProductNodes.addAll(Arrays.asList(products));
    }

    public List<ProductGraphNode> getUsedProductNodes() {
        return Collections.unmodifiableList(usedProductNodes);
    }

    public void setProvidedProducts(final ProductGraphNode... products) {
        providedProductNodes.addAll(Arrays.asList(products));
    }

    public List<ProductGraphNode> getProvidedProductNodes() {
        return Collections.unmodifiableList(providedProductNodes);
    }

}
