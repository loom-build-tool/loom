package jobt;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;

import jobt.config.BuildConfigImpl;
import jobt.config.ConfigReader;

@SuppressWarnings("checkstyle:regexpmultiline")
public class CliProcessor {

    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    private static final String PASTA = "\uD83C\uDF5D";

    private static void printDuration(final int longestKey, final String name,
                                      final long totalDuration, final long watchDuration) {
        final double pct = 100D / totalDuration * watchDuration;
        final String space = String.join("",
            Collections.nCopies(longestKey - name.length(), " "));

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        final double durationSecs = watchDuration / 1_000_000_000D;
        System.out.printf("%s %s: %.2fs (%4.1f%%) %s%n",
            name, space, durationSecs, pct, durationBar);
    }

    public void run(final CommandLine cmd) throws Exception {
        final long startTime = System.nanoTime();

        final JobtProcessor jobtProcessor = new JobtProcessor();

        if (cmd.hasOption("clean")) {
            System.out.println("Cleaning...");
            jobtProcessor.clean();
        }

        if (cmd.getArgList().isEmpty()) {
            // We're done
            return;
        }

        final RuntimeConfigurationImpl runtimeConfiguration =
            new RuntimeConfigurationImpl(!cmd.hasOption("no-cache"));

        Stopwatch.startProcess("Configure logging");
        LogConfiguration.configureLogger();
        Stopwatch.stopProcess();

        jobtProcessor.logMemoryUsage();

        Stopwatch.startProcess("Read configuration");
        final BuildConfigImpl buildConfig = ConfigReader.readConfig(runtimeConfiguration);
        Stopwatch.stopProcess();

        System.out.printf("Initialized configuration for %s version %s%n",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion());

        jobtProcessor.init(buildConfig, runtimeConfiguration);

        for (final String taskName : cmd.getArgs()) {
            System.out.println("Executing " + taskName + "...");
            jobtProcessor.execute(taskName);
        }

        if (cmd.hasOption("stat")) {
            printExecutionStatistics();
        }

        jobtProcessor.logMemoryUsage();

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        System.out.printf(PASTA + "  Cooked your pasta in %.2fs%n", duration);
    }

    private void printExecutionStatistics() {
        System.out.println();
        System.out.println("Execution statistics:");
        System.out.println();

        final int longestKey = Stopwatch.getWatches().keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(10);

        final Stream<Map.Entry<String, Watch>> sorted = Stopwatch.getWatches().entrySet().stream()
            .sorted((o1, o2) ->
                Long.compare(o2.getValue().getDuration(), o1.getValue().getDuration()));

        final long totalDuration = Stopwatch.getTotalDuration();

        sorted.forEach(stringWatchEntry -> {
            final String name = stringWatchEntry.getKey();
            final long watchDuration = stringWatchEntry.getValue().getDuration();
            printDuration(longestKey, name, totalDuration, watchDuration);
        });

        final double totalDurationSecs = totalDuration / 1_000_000_000D;
        System.out.printf("Total: %.2fs (executed in parallel)%n", totalDurationSecs);

        System.out.println();
    }

}
