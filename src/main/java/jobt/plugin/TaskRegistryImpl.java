package jobt.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jobt.api.Task;
import jobt.api.WaitForAllProductsTask;

public class TaskRegistryImpl implements TaskRegistryLookup {

    private final Map<String, ConfiguredTask> taskMap = new ConcurrentHashMap<>();

    @Override
    public void registerTask(final String taskName, final Supplier<Task> taskSupplier,
                             final Set<String> providedProducts, final Set<String> usedProducts,
                             final Set<String> dependencies) {

        Objects.requireNonNull(taskName, "taskName must be specified");
        Objects.requireNonNull(taskSupplier, "taskSupplier must be specified");
        Objects.requireNonNull(providedProducts, "providedProducts must be specified");
        Objects.requireNonNull(usedProducts, "usedProducts must be specified");

        if (taskMap.putIfAbsent(taskName, new ConfiguredTask(taskSupplier, providedProducts,
            usedProducts, dependencies)) != null) {

            throw new IllegalStateException("Task with name " + taskName + " already registered.");
        }
    }

    @Override
    public void registerGoal(final String goalName, final Set<String> usedProducts) {
        Objects.requireNonNull(goalName, "goalName must be specified");
        Objects.requireNonNull(usedProducts, "usedProducts must be specified");

        taskMap.compute(goalName, (name, configuredTask) -> configuredTask != null
            ? configuredTask.addUsedProducts(usedProducts)
            : new ConfiguredTask(WaitForAllProductsTask::new, Collections.singleton(goalName),
                usedProducts, null));
    }

    @Override
    public Set<String> getTaskNames() {
        return Collections.unmodifiableSet(taskMap.keySet());
    }

    @Override
    public ConfiguredTask lookupTask(final String taskName) {
        Objects.requireNonNull(taskName, "taskName is required");
        final ConfiguredTask configuredTask = taskMap.get(taskName);
        if (configuredTask == null) {
            throw new IllegalStateException("Task " + taskName + " doesn't exist");
        }
        return configuredTask;
    }

}
