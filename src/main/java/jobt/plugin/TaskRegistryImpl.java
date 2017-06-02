package jobt.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRegistryImpl implements TaskRegistry {

    private final Map<String, List<Task>> tasks = new HashMap<>();

    @Override
    public void register(final String name, final Task task) {
        List<Task> taskList = this.tasks.get(name);
        if (taskList == null) {
            taskList = new ArrayList<>();
            tasks.put(name, taskList);
        }

        taskList.add(task);
    }

    public List<Task> getTasks(final String name) {
        final List<Task> tasks = this.tasks.get(name);
        return tasks != null ? tasks : Collections.emptyList();
    }

}
