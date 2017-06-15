package jobt.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jobt.api.Task;
import jobt.api.TaskRegistry;

public class TaskRegistryImpl implements TaskRegistry {

    private final Map<String, List<Task>> taskMap = new ConcurrentHashMap<>();

    @Override
    public void register(final String name, final Task task) {
        taskMap.computeIfAbsent(name, k -> new ArrayList<>()).add(task);
    }

    List<Task> getTasks(final String name) {
        final List<Task> tasks = taskMap.get(name);
        return tasks != null ? tasks : Collections.emptyList();
    }

}
