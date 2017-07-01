package jobt.plugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import jobt.api.Task;

public class ConfiguredTask {

    private final Supplier<Task> taskSupplier;
    private final Set<String> providedProducts;
    private final Set<String> usedProducts;
    private final Set<String> dependencies;

    ConfiguredTask(final Supplier<Task> taskSupplier, final Set<String> providedProducts,
                   final Set<String> usedProducts, final Set<String> dependencies) {
        this.taskSupplier = taskSupplier;
        this.providedProducts = new HashSet<>(providedProducts);
        this.usedProducts = new HashSet<>(usedProducts);
        this.dependencies = dependencies != null
            ? new HashSet<>(dependencies) : Collections.emptySet();
    }

    public Supplier<Task> getTaskSupplier() {
        return taskSupplier;
    }

    public Set<String> getProvidedProducts() {
        return Collections.unmodifiableSet(providedProducts);
    }

    public Set<String> getUsedProducts() {
        return Collections.unmodifiableSet(usedProducts);
    }

    ConfiguredTask addUsedProducts(final Set<String> additionalProducts) {
        this.usedProducts.addAll(additionalProducts);
        return this;
    }

    public Set<String> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

}
