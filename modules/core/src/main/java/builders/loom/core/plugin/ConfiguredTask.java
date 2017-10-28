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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import builders.loom.api.BuildContext;
import builders.loom.api.Task;

public class ConfiguredTask {

    private final BuildContext buildContext;
    private final String name;
    private final Set<String> pluginNames;
    private final Supplier<Task> taskSupplier;
    private final String providedProduct;
    private final Set<String> usedProducts;
    private final Set<String> importedProducts;
    private final Set<String> importedAllProducts;
    private final List<String> skipHints;
    private final String description;
    private final TaskType type;

    @SuppressWarnings("checkstyle:parameternumber")
    ConfiguredTask(final BuildContext buildContext, final String name, final String pluginName,
                   final Supplier<Task> taskSupplier, final String providedProduct,
                   final Set<String> usedProducts, final Set<String> importedProducts,
                   final Set<String> importedAllProducts, final List<String> skipHints, final String description,
                   final TaskType type) {
        this.buildContext = buildContext;
        this.name = name;
        this.pluginNames = new HashSet<>(Collections.singletonList(pluginName));
        this.taskSupplier = taskSupplier;
        this.providedProduct = providedProduct;
        this.usedProducts = new HashSet<>(usedProducts);
        this.importedProducts = importedProducts;
        this.importedAllProducts = importedAllProducts;
        this.skipHints = skipHints;
        this.description = description;
        this.type = type;
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }

    public String getName() {
        return name;
    }

    public Set<String> getPluginNames() {
        return Collections.unmodifiableSet(pluginNames);
    }

    public String getPluginName() {
        if (pluginNames.size() != 1) {
            throw new IllegalStateException("Expected exactly 1 Plugin, got " + pluginNames.size());
        }
        return pluginNames.iterator().next();
    }

    public Supplier<Task> getTaskSupplier() {
        return taskSupplier;
    }

    public String getProvidedProduct() {
        return providedProduct;
    }

    public Set<String> getUsedProducts() {
        return Collections.unmodifiableSet(usedProducts);
    }

    public Set<String> getImportedProducts() {
        return Collections.unmodifiableSet(importedProducts);
    }

    ConfiguredTask addUsedProducts(final String pluginName, final Set<String> additionalProducts) {
        pluginNames.add(pluginName);
        this.usedProducts.addAll(additionalProducts);
        return this;
    }

    public Set<String> getImportedAllProducts() {
        return Collections.unmodifiableSet(importedAllProducts);
    }

    public List<String> getSkipHints() {
        return Collections.unmodifiableList(skipHints);
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public boolean isGoal() {
        return type == TaskType.GOAL;
    }

    @Override
    public String toString() {
        return buildContext.getModuleName() + " > " + name;
    }

}
