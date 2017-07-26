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

    public void add(final String taskName, final TaskType type, final TaskStatus taskStatus, final long duration) {
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

        System.out.println();
        System.out.println("Execution statistics (ordered by product completion time):");
        System.out.println();

        for (final Map.Entry<String, ExecutionStatus> entry : durations.entrySet()) {
            printDuration(longestKey, entry.getKey(), totalDuration, entry.getValue());
        }

        System.out.println();
    }

    private static void printDuration(final int longestKey, final String name,
                                      final long totalDuration, final ExecutionStatus executionStatus) {
        final double pct = 100D / totalDuration * executionStatus.getDuration();
        final String space = String.join("",
            Collections.nCopies(longestKey - name.length(), " "));

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        // TODO add task status and task type
        final double durationSecs = executionStatus.getDuration() / 1_000_000_000D;
        System.out.printf("%s %s: %5.2fs (%4.1f%%) %s%n",
            name, space, durationSecs, pct, durationBar);
    }

    private class ExecutionStatus {
        private final TaskStatus taskStatus;
        private final TaskType type;
        private final long duration;

        public ExecutionStatus(final TaskStatus taskStatus, final TaskType type, final long duration) {
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
