package jobt.plugin;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jobt.api.ProductDependenciesAware;
import jobt.api.ProvidedProducts;
import jobt.api.Task;
import jobt.api.TaskRegistry;

public class TaskRegistryImpl implements TaskRegistry {

    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();
    private final Map<String, ProvidedProducts> taskProductsMap = new ConcurrentHashMap<>();

    @Override
    public void register(final String name, final Task task, final ProvidedProducts providedProducts) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(providedProducts);
        if (taskMap.putIfAbsent(name, task) != null) {
            throw new IllegalStateException("Task with name " + name + " already registered.");
        }
        if (taskProductsMap.putIfAbsent(name, providedProducts) != null) {
            throw new IllegalStateException("Task with name " + name + " already configured.");
        }
        // inject products
        // TODO Rework

        if (task instanceof ProductDependenciesAware) {
            task.setProvidedProducts(providedProducts);
        }

    }

    Set<String> taskNames() {
        return taskMap.keySet();
    }

    // TODO rename -> getTask
    Optional<Task> getTasks(final String name) {
        return Optional.ofNullable(taskMap.get(name));
    }

    Optional<ProvidedProducts> getTaskProducts(final String name) {
        return Optional.ofNullable(taskProductsMap.get(name));
    }

}
