package jobt;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.yaml.snakeyaml.Yaml;

import jobt.config.BuildConfigImpl;

@SuppressWarnings("checkstyle:regexpmultiline")
public class CliProcessor {

    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    private static final String PASTA = "\uD83C\uDF5D";

    private static BuildConfigImpl readConfig() throws IOException {
        final Yaml yaml = new Yaml();

        final Path buildFile = Paths.get("build.yml");
        if (!Files.isRegularFile(buildFile)) {
            throw new IOException("No build.yml found");
        }

        try (final Reader resourceAsStream = Files.newBufferedReader(buildFile,
            StandardCharsets.UTF_8)) {
            return yaml.loadAs(resourceAsStream, BuildConfigImpl.class);
        }
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
        System.out.printf("%s %s: %.2fs (%4.1f%%) %s%n",
            name, space, durationSecs, pct, durationBar);
    }

    public void run(final String[] args) throws Exception {
        final long startTime = System.nanoTime();
        final Stopwatch stopwatch = new Stopwatch();

        stopwatch.startProcess("Read configuration");
        final BuildConfigImpl buildConfig = readConfig();
        stopwatch.stopProcess("Read configuration");

        System.out.printf("Initialized configuration for %s version %s",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion());

        stopwatch.startProcess("Parse commandline");
        final Options options = new Options();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);
        stopwatch.stopProcess("Parse commandline");

        stopwatch.startProcess("Initialize plugins");
        final TaskTemplateImpl taskTemplate = new TaskTemplateImpl(buildConfig, stopwatch);
        stopwatch.stopProcess("Initialize plugins");

        for (final String taskName : cmd.getArgs()) {
            taskTemplate.execute(taskName);
        }

        printExecutionStatistics(stopwatch);

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        System.out.printf(PASTA + "  Cooked your pasta in %.2fs%n", duration);
    }

    private void printExecutionStatistics(final Stopwatch stopwatch) {
        System.out.println();
        System.out.println("Execution statistics:");
        System.out.println();

        final int longestKey = stopwatch.getWatches().keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(10);

        final Stream<Map.Entry<String, Watch>> sorted = stopwatch.getWatches().entrySet().stream()
            .sorted((o1, o2) ->
                Long.compare(o2.getValue().getDuration(), o1.getValue().getDuration()));

        final long totalDuration = stopwatch.getTotalDuration().get();

        sorted.forEach(stringWatchEntry -> {
            final String name = stringWatchEntry.getKey();
            final long watchDuration = stringWatchEntry.getValue().getDuration();
            printDuration(longestKey, name, totalDuration, watchDuration);
        });

        System.out.println();
    }

}
