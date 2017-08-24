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
import java.util.Objects;
import java.util.Set;

public class TaskInfo {

    private final String name;
    private final String pluginName;
    private final String description;
    private final TaskType type;
    private final String providedProduct;
    private final Set<String> usedProducts;

    public TaskInfo(final ConfiguredTask configuredTask) {
        this(configuredTask.getProvidedProduct(), configuredTask.getPluginName(),
            configuredTask.getDescription(), configuredTask.getType(),
            configuredTask.getProvidedProduct(), configuredTask.getUsedProducts());
    }

    TaskInfo(final String name, final String pluginName, final String description,
             final TaskType type, final String providedProduct, final Set<String> usedProducts) {
        this.name = name;
        this.pluginName = pluginName;
        this.description = description;
        this.type = type;
        this.providedProduct = providedProduct;
        this.usedProducts = Collections.unmodifiableSet(usedProducts);
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isIntermediateProduct() {
        return type == TaskType.INTERMEDIATE;
    }

    public String getProvidedProduct() {
        return providedProduct;
    }

    public Set<String> getUsedProducts() {
        return usedProducts;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TaskInfo taskInfo = (TaskInfo) o;
        return Objects.equals(name, taskInfo.name)
            && Objects.equals(pluginName, taskInfo.pluginName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, pluginName);
    }

}
