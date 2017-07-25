package builders.loom;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionReport {

    private final Map<String, Long> durations = new LinkedHashMap<>();

    public void add(final String taskName, final long duration) {
        durations.put(taskName, duration);
    }

    public void print() {
        int longestKey = 0;
        long totalDuration = 0;
        for (final Map.Entry<String, Long> entry : durations.entrySet()) {
            if (entry.getKey().length() > longestKey) {
                longestKey = entry.getKey().length();
            }

            totalDuration += entry.getValue();
        }

        System.out.println();
        System.out.println("Execution statistics (ordered by time consumption):");
        System.out.println();

        for (final Map.Entry<String, Long> entry : durations.entrySet()) {
            printDuration(longestKey, entry.getKey(), totalDuration, entry.getValue());
        }

        System.out.println();
    }

    private static void printDuration(final int longestKey, final String name,
                                      final long totalDuration, final long watchDuration) {
        final double pct = 100D / totalDuration * watchDuration;
        final String space = String.join("",
            Collections.nCopies(longestKey - name.length(), " "));

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        final double durationSecs = watchDuration / 1_000_000_000D;
        System.out.printf("%s %s: %5.2fs (%4.1f%%) %s%n",
            name, space, durationSecs, pct, durationBar);
    }

}
