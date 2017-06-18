package jobt;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jobt.config.BuildConfigImpl;
import jobt.config.ConfigReader;
import jobt.util.Stopwatch;
import jobt.util.Watch;

@SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:hideutilityclassconstructor",
    "checkstyle:regexpmultiline", "checkstyle:illegalcatch"})
public class Jobt {

    private static final Path BUILD_FILE = Paths.get("build.yml");

    @SuppressWarnings("checkstyle:avoidescapedunicodecharacters")
    private static final String PASTA = "\uD83C\uDF5D";

    public static void main(final String[] args) {
        final String javaVersion = System.getProperty("java.version");
        System.out.printf("Java Optimized Build Tool v%s (on Java %s)%n",
            Version.getVersion(), javaVersion);

        if (ToolProvider.getSystemJavaCompiler() == null) {
            System.err.println("JDK required (running inside of JRE)");
            System.exit(1);
        }

        if (Files.notExists(BUILD_FILE)) {
            System.err.println("No build.yml found");
            System.exit(1);
        }

        final Options options = new Options()
            .addOption("h", "help", false, "Prints this help")
            .addOption("c", "clean", false, "Clean before execution")
            .addOption("n", "no-cache", false, "Disable all caches (use on CI servers)");

        if (args.length == 0) {
            System.err.println("Nothing to do!");
            printHelp(options);
            System.exit(1);
        }

        final CommandLine cmd = parseCommandLine(args, options);

        if (cmd.hasOption("help")) {
            printHelp(options);
            System.exit(0);
        }

        final long startTime = System.nanoTime();

        try (FileLock ignored = lock()) {
            run(cmd);
        } catch (final Throwable e) {
            e.printStackTrace(System.err);
            System.err.println();
            System.err.println("Build failed");
            System.exit(1);
        }

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        System.out.printf(PASTA + "  Cooked your pasta in %.2fs%n", duration);
    }

    private static CommandLine parseCommandLine(final String[] args, final Options options) {
        try {
            return new DefaultParser().parse(options, args);
        } catch (final ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            System.exit(1);
            throw new IllegalStateException("Unreachable code");
        }
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jobt [option...] [task...]", options);
    }

    private static FileLock lock() throws IOException {
        final FileChannel fileChannel = FileChannel.open(BUILD_FILE,
            StandardOpenOption.READ, StandardOpenOption.WRITE);
        final FileLock fileLock = fileChannel.tryLock();

        if (fileLock == null) {
            System.err.println("Jobt already running");
            System.exit(1);
        }

        return fileLock;
    }

    public static void run(final CommandLine cmd) throws Exception {
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

        printExecutionStatistics();

        jobtProcessor.logMemoryUsage();
    }

    private static void printExecutionStatistics() {
        System.out.println();
        System.out.println("Execution statistics (ordered by time consumption):");
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
        System.out.printf("Total: %.2fs (executed in parallel)%n%n", totalDurationSecs);
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

}
