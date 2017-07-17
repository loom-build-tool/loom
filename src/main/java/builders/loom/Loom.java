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

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.config.BuildConfigWithSettings;
import builders.loom.config.ConfigReader;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.util.Stopwatch;
import builders.loom.util.Watch;

@SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:hideutilityclassconstructor",
    "checkstyle:regexpmultiline", "checkstyle:illegalcatch"})
public class Loom {

    private static final Path BUILD_FILE = Paths.get("build.yml");
    private static final Path LOCK_FILE = Paths.get(".loom.lock");

    public static void main(final String[] args) {
        final Thread ctrlCHook = new Thread(() ->
            AnsiConsole.out().println(Ansi.ansi().reset().newline().fgBrightMagenta()
                .a("Interrupt received - stopping").reset()));

        try {
            init();

            final Options options = buildOptions();
            final CommandLine cmd = parseCommandLine(options, args);

            if (validate(cmd, options)) {
                Runtime.getRuntime().addShutdownHook(ctrlCHook);

                try (FileLock ignored = lock()) {
                    run(cmd);
                }

                Runtime.getRuntime().removeShutdownHook(ctrlCHook);
            }
        } catch (final Throwable e) {
            Runtime.getRuntime().removeShutdownHook(ctrlCHook);
            if (!(e instanceof BuildException)) {
                // BuildExceptions are already logged
                e.printStackTrace(System.err);
            }
            AnsiConsole.err().println(Ansi.ansi().reset().newline().fgBrightRed()
                .format("BUILD FAILED - see %s for details", LogConfiguration.LOOM_BUILD_LOG)
                .reset());
            System.exit(1);
        }

        System.exit(0);
    }

    private static void init() {
        AnsiConsole.out().println(Ansi.ansi().reset().fgCyan()
            .format("Loom Build Tool v%s (on Java %s)",
                Version.getVersion(), System.getProperty("java.version"))
            .reset());

        if (ToolProvider.getSystemJavaCompiler() == null) {
            throw new IllegalStateException("JDK required (running inside of JRE)");
        }

        if (Files.notExists(BUILD_FILE)) {
            throw new IllegalStateException("No build.yml found");
        }
    }

    private static Options buildOptions() {
        return new Options()
            .addOption("h", "help", false, "Prints this help")
            .addOption("c", "clean", false, "Clean before execution")
            .addOption("n", "no-cache", false, "Disable all caches (use on CI servers)")
            .addOption(
                Option.builder("p")
                    .longOpt("products")
                    .numberOfArgs(1)
                    .optionalArg(true)
                    .argName("format")
                    .desc("Show available products (formats: text [default], dot)")
                    .build());
    }

    private static CommandLine parseCommandLine(final Options options, final String[] args) {
        try {
            return new DefaultParser().parse(options, args);
        } catch (final ParseException e) {
            throw new IllegalStateException("Error parsing command line: " + e.getMessage());
        }
    }

    private static boolean validate(final CommandLine cmd, final Options options) {
        if (cmd.hasOption("help")) {
            printHelp(options);
            return false;
        }

        if (!cmd.hasOption("clean") && !cmd.hasOption("products") && cmd.getArgList().isEmpty()) {
            AnsiConsole.out().println(Ansi.ansi().fgBrightYellow()
                .a("No product requested!").reset().newline());
            printHelp(options);
            return false;
        }

        return true;
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("loom [option...] [product...]", options);
    }

    private static FileLock lock() throws IOException {
        if (Files.notExists(LOCK_FILE)) {
            Files.createFile(LOCK_FILE);
        }

        final FileChannel fileChannel = FileChannel.open(LOCK_FILE,
            StandardOpenOption.READ, StandardOpenOption.WRITE);
        final FileLock fileLock = fileChannel.tryLock();

        if (fileLock == null) {
            throw new IllegalStateException("Loom already running - locked by " + LOCK_FILE);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(LOCK_FILE);
            } catch (final IOException ignored) {
                // ignored
            }
        }));

        return fileLock;
    }

    public static void run(final CommandLine cmd) throws Exception {
        final long startTime = System.nanoTime();

        final LoomProcessor loomProcessor = new LoomProcessor();

        if (cmd.hasOption("clean")) {
            AnsiConsole.out().print(Ansi.ansi().a("Cleaning..."));
            loomProcessor.clean();
            AnsiConsole.out().println(Ansi.ansi().a(" ").fgBrightGreen().a("done").reset());

            if (!cmd.hasOption("products") && cmd.getArgList().isEmpty()) {
                return;
            }
        }

        final boolean noCacheMode = cmd.hasOption("no-cache");

        final RuntimeConfigurationImpl runtimeConfiguration =
            new RuntimeConfigurationImpl(!noCacheMode);

        if (noCacheMode) {
            AnsiConsole.out().println(Ansi.ansi().fgBrightYellow().a("Running in no-cache mode")
                .reset());
        }

        configureLogging();

        loomProcessor.logSystemEnvironment();
        loomProcessor.logMemoryUsage();

        final BuildConfigWithSettings buildConfig = readConfig(runtimeConfiguration);

        AnsiConsole.out.println(Ansi.ansi()
            .render("Initialized configuration of @|bold %s:%s|@ version @|bold %s|@",
                buildConfig.getProject().getGroupId(),
                buildConfig.getProject().getArtifactId(),
                buildConfig.getProject().getVersion())
            .reset());

        loomProcessor.init(buildConfig, runtimeConfiguration);

        if (cmd.hasOption("products")) {
            final String format = cmd.getOptionValue("products");
            printProducts(loomProcessor, format);
        }

        if (!cmd.getArgList().isEmpty()) {
            ProgressMonitor.start();

            final Collection<ConfiguredTask> resolvedTasks;
            try {
                resolvedTasks = loomProcessor.execute(cmd.getArgList());
            } finally {
                ProgressMonitor.stop();
            }

            loomProcessor.printProductInfos(resolvedTasks);

            printExecutionStatistics();

            final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;

            AnsiConsole.out().print(Ansi.ansi().reset().fgBrightGreen()
                .a("BUILD SUCCESSFUL").reset()
                .format(" in %.2fs%n", duration));
        }

        loomProcessor.logMemoryUsage();
    }

    private static void configureLogging() {
        Stopwatch.startProcess("Configure logging");
        LogConfiguration.configureLogger();
        Runtime.getRuntime().addShutdownHook(new Thread(LogConfiguration::stop));
        Stopwatch.stopProcess();
    }

    private static BuildConfigWithSettings readConfig(final RuntimeConfigurationImpl
                                                          runtimeConfiguration) throws IOException {
        Stopwatch.startProcess("Read configuration");
        final BuildConfigWithSettings buildConfig = ConfigReader.readConfig(runtimeConfiguration, Paths.get("build.yml"), "base");
        Stopwatch.stopProcess();
        return buildConfig;
    }

    private static void printProducts(final LoomProcessor loomProcessor, final String format) {
        if (format == null || "text".equals(format)) {
            loomProcessor.generateTextProductOverview();
        } else if ("dot".equals(format)) {
            loomProcessor.generateDotProductOverview();
        } else {
            throw new IllegalStateException("Unknown format: " + format);
        }
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
