package jobt.plugin;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPlugin implements Plugin {

    private final Map<String, Task> taskClasses = new HashMap<>();

    protected void registerTask(final String taskName, final Task task) {
        taskClasses.put(taskName, task);
    }

    public void run(final String task) throws Exception {
        final Task t = taskClasses.get(task);
        if (t != null) {
            t.run();
        }
    }

}
