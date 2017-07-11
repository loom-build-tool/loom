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

    private final String pluginName;
    private final Supplier<Task> taskSupplier;
    private final String providedProduct;
    private final Set<String> usedProducts;

    ConfiguredTask(final String pluginName, final Supplier<Task> taskSupplier,
                   final String providedProduct, final Set<String> usedProducts) {
        this.pluginName = pluginName;
        this.taskSupplier = taskSupplier;
        this.providedProduct = providedProduct;
        this.usedProducts = new HashSet<>(usedProducts);
    }

    public String getPluginName() {
        return pluginName;
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

    ConfiguredTask addUsedProducts(final Set<String> additionalProducts) {
        this.usedProducts.addAll(additionalProducts);
        return this;
    }

}
