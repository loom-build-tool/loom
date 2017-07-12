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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import builders.loom.api.Task;

public class ConfiguredTask {

    private final String name;
    private final Set<String> pluginNames;
    private final Supplier<Task> taskSupplier;
    private final String providedProduct;
    private final boolean intermediateProduct;
    private final Set<String> usedProducts;
    private final boolean isGoal;
    private final String description;

    ConfiguredTask(final String name, final String pluginName, final Supplier<Task> taskSupplier,
                   final String providedProduct, final boolean intermediateProduct,
                   final Set<String> usedProducts, final boolean isGoal, final String description) {
        this.name = name;
        this.pluginNames = new HashSet<>(Collections.singletonList(pluginName));
        this.taskSupplier = taskSupplier;
        this.providedProduct = providedProduct;
        this.intermediateProduct = intermediateProduct;
        this.usedProducts = new HashSet<>(usedProducts);
        this.isGoal = isGoal;
        this.description = description;
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

    public boolean isIntermediateProduct() {
        return intermediateProduct;
    }

    public Set<String> getUsedProducts() {
        return Collections.unmodifiableSet(usedProducts);
    }

    ConfiguredTask addUsedProducts(final String pluginName, final Set<String> additionalProducts) {
        pluginNames.add(pluginName);
        this.usedProducts.addAll(additionalProducts);
        return this;
    }

    public boolean isGoal() {
        return isGoal;
    }

    public String getDescription() {
        return description;
    }

}
