package builders.loom.plugin;

import java.util.HashMap;
import java.util.Map;

public final class TaskUtil {

    private TaskUtil() {
    }

    public static Map<String, String> buildInvertedProducersMap(
        final TaskRegistryLookup taskRegistry) {
        final Map<String, String> producersMap = new HashMap<>();

        for (final String taskName : taskRegistry.getTaskNames()) {
            final ConfiguredTask configuredTask = taskRegistry.lookupTask(taskName);
            for (final String providedProduct : configuredTask.getProvidedProducts()) {
                final String oldTaskName = producersMap.putIfAbsent(providedProduct, taskName);
                if (oldTaskName != null) {
                    throw new IllegalStateException("Product " + providedProduct + " provided by "
                        + taskName + " but was already provided by " + oldTaskName);
                }
            }
        }

        return producersMap;
    }

}
