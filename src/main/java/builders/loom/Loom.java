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
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.api.LoomPaths;
import builders.loom.util.FileUtil;

@SuppressWarnings({"checkstyle:hideutilityclassconstructor",
    "checkstyle:classdataabstractioncoupling"})
public class Loom {

    // hold System.err, because it will be changed by StdOut2SLF4J
    private static final PrintStream SYSTEM_ERR = System.err;

    private static boolean buildExecuted;

    @SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:illegalcatch",
        "checkstyle:regexpmultiline"})
    public static void main(final String[] args) {
        try {
            mainWithoutExit(args);
            System.exit(0);
        } catch (final Throwable e) {
            System.exit(1);
        }
    }

    @SuppressWarnings({"checkstyle:illegalcatch", "checkstyle:illegalthrows"})
    private static void mainWithoutExit(final String[] args) throws Throwable {
        final long startTime = System.nanoTime();

        final Path projectBaseDir = determineProjectBaseDir();
        final Path lockFile = projectBaseDir.resolve(".loom.lock");
        final Path logFile = LoomPaths.loomDir(projectBaseDir).resolve("build.log");

        final Thread ctrlCHook = new Thread(() ->
            AnsiConsole.err().println(Ansi.ansi().reset().newline().fgBrightMagenta()
                .a("Interrupt received - stopping").reset()));

        try {
            init();

            final Options options = buildOptions();
            final CommandLine cmd = parseCommandLine(options, args);

            if (validate(cmd, options)) {
                Runtime.getRuntime().addShutdownHook(ctrlCHook);

                try (FileLock ignored = lock(lockFile)) {
                    run(projectBaseDir, logFile, cmd);
                }
            }
        } catch (final Throwable e) {
            if (!(e instanceof BuildException)) {
                // BuildExceptions are already logged
                e.printStackTrace(SYSTEM_ERR);
            }
            if (buildExecuted) {
                printFailed(logFile);
            }
            throw e;
        } finally {
            Runtime.getRuntime().removeShutdownHook(ctrlCHook);
        }

        if (buildExecuted) {
            printSuccess(startTime);
        }
    }

    private static Path determineProjectBaseDir() {
        final String projectHome = System.getProperty("loom.project_dir");

        if (projectHome == null) {
            throw new IllegalStateException("No loom.project_dir set");
        }

        final Path projectBaseDir = Paths.get(projectHome);

        if (!Files.isDirectory(projectBaseDir)) {
            throw new IllegalStateException("Directory doesn't exist: " + projectBaseDir);
        }

        return projectBaseDir;
    }

    private static void init() {
        final String loomVersion = Version.getVersion();
        AnsiConsole.out().println(Ansi.ansi().reset().fgCyan()
            .format("Loom Build Tool v%s (on Java %s)",
                loomVersion, Runtime.version())
            .reset());

        if (isPreRelease(loomVersion)) {
            AnsiConsole.out().println(Ansi.ansi().reset().fgBrightYellow()
                .a("You're using an unstable version of Loom -- use it with caution!")
                .reset());
        }

        if (ToolProvider.getSystemJavaCompiler() == null) {
            throw new IllegalStateException("JDK required (running inside of JRE)");
        }
    }

    private static boolean isPreRelease(final String loomVersion) {
        // pre-releases are determined by a hyphen (in accordance to semantic versioning)
        return loomVersion.contains("-");
    }

    private static Options buildOptions() {
        return new Options()
            .addOption("h", "help", false, "Prints this help")
            .addOption("c", "clean", false, "Clean before execution")
            .addOption("n", "no-cache", false, "Disable all caches (use on CI servers)")
            .addOption(
                Option.builder("a")
                    .longOpt("artifact-version")
                    .numberOfArgs(1)
                    .optionalArg(false)
                    .argName("version")
                    .desc("Defines the version to use for artifact creation")
                    .build())
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

    private static FileLock lock(final Path lockFile) throws IOException {
        if (Files.notExists(lockFile)) {
            Files.createFile(lockFile);
        }

        final FileChannel fileChannel = FileChannel.open(lockFile,
            StandardOpenOption.READ, StandardOpenOption.WRITE);
        final FileLock fileLock = fileChannel.tryLock();

        if (fileLock == null) {
            throw new IllegalStateException("Loom already running - locked by " + lockFile);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(lockFile);
            } catch (final IOException ignored) {
                // ignored
            }
        }));

        return fileLock;
    }

    private static void run(final Path projectBaseDir, final Path logFile,
                               final CommandLine cmd) throws Exception {
        if (cmd.hasOption("clean")) {
            AnsiConsole.out().print(Ansi.ansi().a("Cleaning..."));
            clean(projectBaseDir);
            AnsiConsole.out().println(Ansi.ansi().a(" ").fgBrightGreen().a("done").reset());

            if (!cmd.hasOption("products") && cmd.getArgList().isEmpty()) {
                return;
            }
        }

        configureLogging(logFile);

        final LoomProcessor loomProcessor = new LoomProcessor();
        loomProcessor.logSystemEnvironment();
        loomProcessor.logMemoryUsage();

        final boolean noCacheMode = cmd.hasOption("no-cache");

        final RuntimeConfigurationImpl runtimeConfiguration =
            new RuntimeConfigurationImpl(projectBaseDir, !noCacheMode,
                cmd.getOptionValue("artifact-version"),
                loomProcessor.isModuleBuild(projectBaseDir));

        printRuntimeConfiguration(runtimeConfiguration);

        if (noCacheMode) {
            AnsiConsole.out().println(Ansi.ansi().fgBrightYellow().a("Running in no-cache mode")
                .reset());
        }

        loomProcessor.init(runtimeConfiguration);

        if (cmd.hasOption("products")) {
            final String format = cmd.getOptionValue("products");
            printProducts(runtimeConfiguration, loomProcessor, format);
        }

        if (!cmd.getArgList().isEmpty()) {
            buildExecuted = true;

            ProgressMonitor.start();

            final Optional<ExecutionReport> optExecutionReport;
            try {
                optExecutionReport = loomProcessor.execute(cmd.getArgList());
            } finally {
                ProgressMonitor.stop();
            }

            if (optExecutionReport.isPresent()) {
                final ExecutionReport executionReport = optExecutionReport.get();
                new ProductReportPrinter(loomProcessor.getModuleRunner()).print(executionReport);
                new ExecutionReportPrinter().print(executionReport);
            }
        }

        loomProcessor.logMemoryUsage();
    }

    private static void clean(final Path projectBaseDir) {
        FileUtil.deleteDirectoryRecursively(LoomPaths.loomDir(projectBaseDir), true);
        FileUtil.deleteDirectoryRecursively(LoomPaths.buildDir(projectBaseDir), true);
    }

    private static void configureLogging(final Path logFile) {
        LogConfiguration.configureLogger(logFile);
        Runtime.getRuntime().addShutdownHook(new Thread(LogConfiguration::stop));
    }

    private static void printRuntimeConfiguration(final RuntimeConfigurationImpl rtConfig) {
        final Ansi a = Ansi.ansi()
            .a("Initialized runtime configuration");

        if (rtConfig.getVersion() != null) {
            a.a(' ')
                .bold()
                .a(rtConfig.getVersion())
                .boldOff();
        }

        AnsiConsole.out.println(a);
    }

    private static void printProducts(final RuntimeConfigurationImpl runtimeConfiguration,
                                      final LoomProcessor loomProcessor, final String format) {
        if (format == null || "text".equals(format)) {
            loomProcessor.generateTextProductOverview();
        } else if ("dot".equals(format)) {
            loomProcessor.generateDotProductOverview(runtimeConfiguration);
        } else {
            throw new IllegalStateException("Unknown format: " + format);
        }
    }

    private static void printFailed(final Path logFile) {
        AnsiConsole.err().println(Ansi.ansi().reset().newline()
            .fgBrightRed().bold().a("BUILD FAILED").reset()
            .render(" - see @|bold %s|@ for details", logFile)
            .reset()
            .newline());
    }

    private static void printSuccess(final long startTime) {
        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;

        AnsiConsole.out().println(Ansi.ansi().reset().newline()
            .fgBrightGreen().bold().a("BUILD SUCCESSFUL").reset()
            .a(" in ")
            .format("%.2fs", duration)
            .newline());
    }

}
