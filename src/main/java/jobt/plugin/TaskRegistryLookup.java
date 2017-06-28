package jobt.plugin;

import java.util.Set;

import jobt.api.TaskRegistry;

public interface TaskRegistryLookup extends TaskRegistry {

    Set<String> getTaskNames();

    ConfiguredTask lookupTask(String taskName);

}
