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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import builders.loom.api.Task;
import builders.loom.api.WaitForAllProductsTask;

public class TaskRegistryImpl implements TaskRegistryLookup {

    private final Map<String, ConfiguredTask> taskMap = new ConcurrentHashMap<>();

    @Override
    public void registerTask(final String taskName, final Supplier<Task> taskSupplier,
                             final Set<String> providedProducts, final Set<String> usedProducts) {

        Objects.requireNonNull(taskName, "taskName must be specified");
        Objects.requireNonNull(taskSupplier, "taskSupplier missing on task <" + taskName + ">");
        Objects.requireNonNull(providedProducts,
            "providedProducts missing on task <" + taskName + ">");
        Objects.requireNonNull(usedProducts, "usedProducts missing on task <" + taskName + ">");

        if (taskMap.putIfAbsent(taskName, new ConfiguredTask(taskSupplier, providedProducts,
            usedProducts)) != null) {

            throw new IllegalStateException("Task with name " + taskName + " already registered.");
        }
    }

    @Override
    public void registerGoal(final String goalName, final Set<String> usedProducts) {
        Objects.requireNonNull(goalName, "goalName must be specified");
        Objects.requireNonNull(usedProducts,
            "usedProducts missing on goal <" + goalName + ">");

        taskMap.compute(goalName, (name, configuredTask) -> configuredTask != null
            ? configuredTask.addUsedProducts(usedProducts)
            : new ConfiguredTask(WaitForAllProductsTask::new, Collections.singleton(goalName),
                usedProducts));
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
