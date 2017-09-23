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

package builders.loom.core.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import builders.loom.api.BuildContext;
import builders.loom.api.GoalTask;
import builders.loom.api.Task;
import builders.loom.core.misc.DirectedGraph;

public class TaskRegistryImpl implements TaskRegistryLookup {

    private final BuildContext buildContext;
    private final Map<String, ConfiguredTask> taskMap = new ConcurrentHashMap<>();

    public TaskRegistryImpl(final BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    @Override
    public void registerTask(final String pluginName, final String taskName,
                             final Supplier<Task> taskSupplier, final String providedProduct,
                             final boolean intermediateProduct, final Set<String> usedProducts,
                             final Set<String> importedProducts,
                             final Set<String> importedAllProducts,
                             final List<String> skipHints,
                             final String description) {

        Objects.requireNonNull(taskName, "taskName must be specified");
        Objects.requireNonNull(taskSupplier, "taskSupplier missing on task <" + taskName + ">");
        Objects.requireNonNull(providedProduct,
            "providedProducts missing on task <" + taskName + ">");
        Objects.requireNonNull(usedProducts, "usedProducts missing on task <" + taskName + ">");
        Objects.requireNonNull(importedProducts,
            "importedProducts missing on task <" + taskName + ">");
        Objects.requireNonNull(importedAllProducts,
            "importedProducts missing on task <" + taskName + ">");
        Objects.requireNonNull(description, "description missing on task <" + taskName + ">");

        if (usedProducts.contains(null)) {
            throw new IllegalArgumentException("usedProducts contains null on task <"
                + taskName + ">");
        }

        if (importedProducts.contains(null)) {
            throw new IllegalArgumentException("importedProducts contains null on task <"
                + taskName + ">");
        }

        if (importedAllProducts.contains(null)) {
            throw new IllegalArgumentException("importedAllProducts contains null on task <"
                + taskName + ">");
        }

        final TaskType type = intermediateProduct ? TaskType.INTERMEDIATE : TaskType.STANDARD;
        if (taskMap.putIfAbsent(taskName, new ConfiguredTask(buildContext, taskName, pluginName,
            taskSupplier, providedProduct, usedProducts, importedProducts, importedAllProducts,
            skipHints,
            description, type)) != null) {

            throw new IllegalStateException("Task with name " + taskName + " already registered.");
        }
    }

    @Override
    public void registerGoal(final String pluginName, final String goalName,
                             final Set<String> usedProducts) {

        Objects.requireNonNull(goalName, "goalName must be specified");
        Objects.requireNonNull(usedProducts,
            "usedProducts missing on goal <" + goalName + ">");

        if (usedProducts.contains(null)) {
            throw new IllegalArgumentException("usedProducts contains null on goal <"
                + goalName + ">");
        }

        final BiFunction<String, ConfiguredTask, ConfiguredTask> fn = (name, configuredTask) ->
            configuredTask != null
                ? configuredTask.addUsedProducts(pluginName, usedProducts)
                : new ConfiguredTask(
                    buildContext, name, pluginName, () -> new GoalTask(usedProducts),
                goalName, usedProducts,
                Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), null,
                TaskType.GOAL);

        taskMap.compute(goalName, fn);
    }

    @Override
    public Collection<ConfiguredTask> configuredTasks() {
        return Collections.unmodifiableCollection(taskMap.values());
    }

    @Override
    public Collection<ConfiguredTask> resolve(final Set<String> productIds) {
        final DirectedGraph<ConfiguredTask> graph = new DirectedGraph<>();

        final Map<String, ConfiguredTask> products = new HashMap<>();
        for (final ConfiguredTask configuredTask : taskMap.values()) {
            products.put(configuredTask.getProvidedProduct(), configuredTask);
            graph.addNode(configuredTask);
        }

        for (final ConfiguredTask configuredTask : taskMap.values()) {
            for (final String usedProduct : configuredTask.getUsedProducts()) {
                graph.addEdge(configuredTask, products.get(usedProduct));
            }
        }

        final List<ConfiguredTask> tasksToResolve = productIds.stream()
            .map(products::get)
            .collect(Collectors.toList());

        return graph.resolve(tasksToResolve);
    }

}
