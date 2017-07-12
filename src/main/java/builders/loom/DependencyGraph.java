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

package builders.loom;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.TaskRegistryLookup;

public class DependencyGraph {

    private final Map<String, ProductNode> productNodeMap = new HashMap<>();

    public DependencyGraph(final TaskRegistryLookup taskRegistry) {
        taskRegistry.configuredTasks().forEach(this::add);
        build();
    }

    private void add(final ConfiguredTask task) {
        final String providedProduct = task.getProvidedProduct();
        if (productNodeMap.put(providedProduct,
            new ProductNode(task)) != null) {
            throw new IllegalStateException("Product " + providedProduct + " already registered");
        }
    }

    private void build() {
        for (final ProductNode productNode : productNodeMap.values()) {
            final Set<String> usedProducts = productNode.getProvider().getUsedProducts();
            productNode.addAll(products(usedProducts));
        }
    }

    public Set<ConfiguredTask> resolve(final Set<String> productIds) {
        return new LinkedHashSet<>(productIds.stream()
            .flatMap(id -> productNodeMap.get(id).getAllDependentProducts().stream())
            .map(ProductNode::getProvider)
            .collect(Collectors.toList()));
    }

    private Set<ProductNode> products(final Set<String> usedProducts) {
        return new LinkedHashSet<>(usedProducts.stream()
            .map(productNodeMap::get)
            .collect(Collectors.toList()));
    }

}
