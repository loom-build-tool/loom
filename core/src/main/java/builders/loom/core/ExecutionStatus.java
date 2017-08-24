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

package builders.loom.core;

import builders.loom.api.TaskStatus;
import builders.loom.core.plugin.TaskType;

public class ExecutionStatus {

    private final TaskStatus taskStatus;
    private final TaskType type;
    private final long duration;

    ExecutionStatus(final TaskStatus taskStatus, final TaskType type,
                    final long duration) {
        this.taskStatus = taskStatus;
        this.type = type;
        this.duration = duration;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public TaskType getType() {
        return type;
    }

    public long getDuration() {
        return duration;
    }

}
