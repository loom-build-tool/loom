package jobt.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jobt.api.ProvidedProducts;
import jobt.api.Task;
import jobt.api.TaskRegistry;

public class TaskRegistryImpl implements TaskRegistry {

    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();
    private final Map<String, ProvidedProducts> taskProductsMap = new ConcurrentHashMap<>();

    @Override
    public void register(
        final String name, final Task task, final ProvidedProducts providedProducts) {
        Objects.requireNonNull(task);
        Objects.requireNonNull(providedProducts);
        if (taskMap.putIfAbsent(name, task) != null) {
            throw new IllegalStateException("Task with name " + name + " already registered.");
        }
        if (taskProductsMap.putIfAbsent(name, providedProducts) != null) {
            throw new IllegalStateException("Task with name " + name + " already configured.");
        }

        task.setProvidedProducts(providedProducts);

    }

    Set<String> taskNames() {
        return Collections.unmodifiableSet(taskMap.keySet());
    }

    Task lookupTask(final String name) {
        return Objects.requireNonNull(taskMap.get(name));
    }

    ProvidedProducts lookupTaskProducts(final String name) {
        return Objects.requireNonNull(taskProductsMap.get(name));
    }

}
