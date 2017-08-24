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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import builders.loom.api.TaskStatus;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.TaskType;

public class ExecutionReport {

    private final Map<String, ExecutionStatus> durations = new LinkedHashMap<>();
    private final List<ConfiguredTask> resolvedTasks;

    public ExecutionReport(final List<ConfiguredTask> resolvedTasks) {
        this.resolvedTasks = resolvedTasks;
    }

    public List<ConfiguredTask> getResolvedTasks() {
        return resolvedTasks;
    }

    public void add(final String taskName, final TaskType type, final TaskStatus taskStatus,
                    final long duration) {
        durations.put(taskName, new ExecutionStatus(taskStatus, type, duration));
    }

    public Map<String, ExecutionStatus> getDurations() {
        return Collections.unmodifiableMap(durations);
    }

}
