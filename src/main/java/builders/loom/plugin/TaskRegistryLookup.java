package builders.loom.plugin;

import java.util.Set;

import builders.loom.api.TaskRegistry;

public interface TaskRegistryLookup extends TaskRegistry {

    Set<String> getTaskNames();

    ConfiguredTask lookupTask(String taskName);

}
