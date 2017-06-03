package jobt;

import java.io.IOException;

@SuppressWarnings({"checkstyle:uncommentedmain", "checkstyle:hideutilityclassconstructor",
    "checkstyle:regexpmultiline", "checkstyle:illegalcatch"})
public class Jobt {

    public static void main(final String[] args) throws IOException {
        final String javaVersion = System.getProperty("java.version");
        System.out.println("Java Optimized Build Tool v1.0.0 (on Java " + javaVersion + ")");

        try {
            new CliProcessor().run(args);
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.err.println();
            System.err.println("Build failed");
            System.exit(1);
        }
    }

}
