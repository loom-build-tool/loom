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

package builders.loom.cli;

import java.io.PrintStream;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.tools.ToolProvider;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.api.LoomPaths;
import builders.loom.core.BuildException;
import builders.loom.core.ExecutionReport;
import builders.loom.core.LoomProcessor;
import builders.loom.core.LoomVersion;
import builders.loom.core.ProgressMonitor;
import builders.loom.core.RuntimeConfigurationImpl;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.util.FileUtil;

@SuppressWarnings("checkstyle:hideutilityclassconstructor")
public final class Loom {

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

            final LoomCommand cmd = new LoomCommand(args);

            if (validate(cmd)) {
                Runtime.getRuntime().addShutdownHook(ctrlCHook);

                cmd.getSystemProperties().forEach(System::setProperty);

                try (FileLock ignored = FileLockUtil.lock(lockFile)) {
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
        final String loomVersion = LoomVersion.getVersion();
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

    private static boolean validate(final LoomCommand cmd) {
        if (cmd.isHelpFlag()) {
            cmd.printHelp();
            return false;
        }

        if (!cmd.isAnyOperationRequested()) {
            AnsiConsole.out().println(Ansi.ansi()
                .newline()
                .fgBrightRed()
                .a("No operation requested!").reset().newline());
            cmd.printHelp();
            return false;
        }

        return true;
    }

    private static boolean isPreRelease(final String loomVersion) {
        // pre-releases are determined by a hyphen (in accordance to semantic versioning)
        return loomVersion.contains("-");
    }

    private static void run(final Path projectBaseDir, final Path logFile, final LoomCommand cmd)
        throws Exception {

        if (cmd.isCleanFlag() || cmd.isNoCacheFlag()) {
            AnsiConsole.out().print(Ansi.ansi().a("Cleaning..."));
            clean(projectBaseDir);
            AnsiConsole.out().println(Ansi.ansi().a(" ").fgBrightGreen().a("done").reset());

            if (cmd.getPrintProducts() == null && cmd.getProducts().isEmpty()) {
                return;
            }
        }

        configureLogging(logFile);

        final LoomProcessor loomProcessor = new LoomProcessor();
        loomProcessor.logSystemEnvironment();
        loomProcessor.logMemoryUsage();

        final boolean noCacheMode = cmd.isNoCacheFlag();

        final RuntimeConfigurationImpl runtimeConfiguration =
            new RuntimeConfigurationImpl(projectBaseDir, !noCacheMode,
                cmd.getRelease(),
                loomProcessor.isModuleBuild(projectBaseDir));

        printRuntimeConfiguration(runtimeConfiguration);

        if (noCacheMode) {
            AnsiConsole.out().println(Ansi.ansi()
                .fgBrightYellow().a("Running in no-cache mode").reset());
        }

        final ProgressMonitor progressMonitor = new ConsoleProgressMonitor();

        loomProcessor.init(runtimeConfiguration, progressMonitor);

        if (cmd.getPrintProducts() != null) {
            printProducts(runtimeConfiguration, loomProcessor, cmd.getPrintProducts());
        }

        if (!cmd.getProducts().isEmpty()) {
            buildExecuted = true;

            progressMonitor.start();

            final List<ConfiguredTask> configuredTasks =
                loomProcessor.resolveTasks(cmd.getProducts());

            if (!configuredTasks.isEmpty()) {
                final ExecutionReport executionReport;
                try {
                    executionReport = loomProcessor.execute(configuredTasks);
                } finally {
                    progressMonitor.stop();

                    new ProductReportPrinter(runtimeConfiguration, loomProcessor.getModuleRunner())
                        .print(configuredTasks);
                }

                new ExecutionReportPrinter().print(executionReport.getDurations());
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
        final Ansi a = Ansi.ansi().a("Initialized runtime configuration");

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

        switch (format) {
            case "text":
                TextOutput.generate(loomProcessor.getModuleRunner());
                break;
            case "dot":
                generateDotProductOverview(runtimeConfiguration, loomProcessor);
                break;
            default:
                throw new IllegalStateException("Unknown format: " + format);
        }
    }

    private static void generateDotProductOverview(final RuntimeConfigurationImpl rtConfig,
                                                   final LoomProcessor loomProcessor) {

        final Path dotFile = LoomPaths.reportDir(rtConfig.getProjectBaseDir(), "graphviz")
            .resolve("loom-products.dot");

        loomProcessor.generateDotProductOverview(dotFile);

        AnsiConsole.out().println(Ansi.ansi()
            .a("Products overview written to ")
            .bold().a(dotFile).boldOff()
            .newline()
            .a("Use Graphviz to visualize: ")
            .bold().format("`dot -Tpng %s > loom-products.png`", dotFile).boldOff()
            .newline());
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
