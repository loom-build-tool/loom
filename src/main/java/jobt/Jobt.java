package jobt;

import javax.tools.ToolProvider;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

@SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:hideutilityclassconstructor",
    "checkstyle:regexpmultiline", "checkstyle:illegalcatch"})
public class Jobt {

    public static void main(final String[] args) {
        final String javaVersion = System.getProperty("java.version");
        System.out.println("Java Optimized Build Tool v" + Version.getVersion()
            + " (on Java " + javaVersion + ")");

        if (ToolProvider.getSystemJavaCompiler() == null) {
            System.err.println("JDK required (running inside of JRE)");
            System.exit(1);
        }

        final Options options = new Options()
            .addOption("h", "help", false, "Prints this help")
            .addOption("s", "stat", false, "Enabled statistics output")
            .addOption("c", "clean", false, "Clean before execution");

        if (args.length == 0) {
            System.err.println("Nothing to do!");
            printHelp(options);
            System.exit(1);
        }

        try {
            final CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("help")) {
                printHelp(options);
                System.exit(0);
            }

            new CliProcessor().run(cmd);
        } catch (final ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            System.exit(1);
        } catch (final Throwable e) {
            e.printStackTrace(System.err);
            System.err.println();
            System.err.println("Build failed");
            System.exit(1);
        }
    }

    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("jobt [option...] [task...]", options);
    }

}
