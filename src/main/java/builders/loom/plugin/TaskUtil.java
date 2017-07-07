/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
