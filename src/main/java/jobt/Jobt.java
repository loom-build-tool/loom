package jobt;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:hideutilityclassconstructor",
    "checkstyle:regexpmultiline", "checkstyle:illegalcatch"})
public class Jobt {

    private static final Path BUILD_FILE = Paths.get("build.yml");

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
            .addOption("s", "stat", false, "Enabled statistics output")
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

        try (FileLock ignored = lock()) {
            new CliProcessor().run(cmd);
        } catch (final Throwable e) {
            e.printStackTrace(System.err);
            System.err.println();
            System.err.println("Build failed");
            System.exit(1);
        }
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

}
