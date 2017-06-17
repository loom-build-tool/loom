package jobt.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jobt.api.Task;
import jobt.api.TaskRegistry;

public class TaskRegistryImpl implements TaskRegistry {

    private final Map<String, Task> taskMap = new ConcurrentHashMap<>();

    @Override
    public void register(final String name, final Task task) {
        if (taskMap.putIfAbsent(name, task) != null) {
            throw new IllegalStateException("Task with name " + name + " already registered.");
        }
    }

    Optional<Task> getTasks(final String name) {
        return Optional.ofNullable(taskMap.get(name));
    }

}
