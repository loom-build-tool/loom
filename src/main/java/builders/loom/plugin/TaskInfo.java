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

public class TaskInfo {

    private final String name;
    private final String pluginName;
    private final String description;
    private final TaskType type;

    public TaskInfo(final ConfiguredTask configuredTask) {
        this(configuredTask.getProvidedProduct(), configuredTask.getPluginName(), configuredTask.getDescription(), configuredTask.getType());
    }


    TaskInfo(final String name, final String pluginName,
             final String description, final TaskType type) {
        this.name = name;
        this.pluginName = pluginName;
        this.description = description;
        this.type = type;
    }

    // FIXME provided productID
    public String getName() {
        return name;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getDescription() {
        return description;
    }

    public TaskType getType() {
        return type;
    }

    public boolean isIntermediateProduct() {
        return type == TaskType.INTERMEDIATE;
    }

    public boolean isGoal() {
        return type == TaskType.GOAL;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pluginName == null) ? 0 : pluginName.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TaskInfo other = (TaskInfo) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (pluginName == null) {
            if (other.pluginName != null) {
                return false;
            }
        } else if (!pluginName.equals(other.pluginName)) {
            return false;
        }
        return true;
    }

}
