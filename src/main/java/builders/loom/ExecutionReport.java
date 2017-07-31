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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

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

    public void print() {
        int longestKey = 0;
        long totalDuration = 0;
        for (final Map.Entry<String, ExecutionStatus> entry : durations.entrySet()) {
            if (entry.getKey().length() > longestKey) {
                longestKey = entry.getKey().length();
            }

            totalDuration += entry.getValue().getDuration();
        }

        AnsiConsole.out().println(Ansi.ansi()
            .newline()
            .bold()
            .a("Execution statistics (ordered by product completion time):")
            .reset()
            .newline());

        for (final Map.Entry<String, ExecutionStatus> entry : durations.entrySet()) {
            printDuration(longestKey, entry.getKey(), totalDuration, entry.getValue());
        }

        AnsiConsole.out().println();
    }

    private static void printDuration(final int longestKey, final String name,
                                      final long totalDuration,
                                      final ExecutionStatus executionStatus) {
        final double pct = 100D / totalDuration * executionStatus.getDuration();
        final String space = String.join("",
            Collections.nCopies(longestKey - name.length(), " "));

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        final double durationSecs = executionStatus.getDuration() / 1_000_000_000D;

        final Ansi a = Ansi.ansi();

        if (executionStatus.getType() == TaskType.GOAL) {
            a.fgBrightYellow().a("Goal");
        } else {
            a.fgBrightCyan().a("Task");
        }

        a.reset()
            .a(' ')
            .a(statusColor(executionStatus.getTaskStatus()))
            .a(' ')
            .a(name)
            .a(' ')
            .a(space)
            .format(" %5.2fs", durationSecs)
            .format(" (%4.1f%%)", pct)
            .a(' ')
            .a(durationBar);

        AnsiConsole.out().println(a);
    }

    private static Ansi statusColor(final TaskStatus taskStatus) {
        switch (taskStatus) {
            case SKIP:
                return Ansi.ansi().fgBrightBlack().a("<SK>").fgDefault();
            case UP_TO_DATE:
                return Ansi.ansi().fgBrightMagenta().a("<UP>").fgDefault();
            case OK:
                return Ansi.ansi().fgBrightGreen().a("<OK>").fgDefault();
            default:
                throw new IllegalStateException("Unknown status " + taskStatus);
        }
    }

    private class ExecutionStatus {
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

}
