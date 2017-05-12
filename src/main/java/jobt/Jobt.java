package jobt;

import java.util.StringTokenizer;

public class Jobt {

    public static void main(final String[] args) {
        final String javaVersion = System.getProperty("java.version");
        System.out.println("Java Optimized Build Tool v1.0.0 (on Java " + javaVersion + ")");

        checkJavaVersion(javaVersion);

        try {
            new CliProcessor(args).run();
        } catch (final Exception e) {
            System.err.println("Build failed - " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static void checkJavaVersion(final String javaVersion) {
        // Use StringTokenizer for JDK 1.0 support

        final StringTokenizer tokenizer = new StringTokenizer(javaVersion, ".", false);

        if (!tokenizer.hasMoreElements()) {
            System.err.println("Unrecognized Java version: " + javaVersion);
            System.exit(1);
        }

        try {
            int majorVersion = Integer.parseInt(tokenizer.nextToken());

            if (majorVersion < 1) {
                System.err.println("Unknown Java version: " + javaVersion);
                System.exit(1);
            }

            if (majorVersion > 1) {
                return;
            }

            final int minorVersion = Integer.parseInt(tokenizer.nextToken());

            if (minorVersion < 8) {
                System.err.println("Unsupported Java version: " + javaVersion);
                System.err.println("Minimum version required: 1.8");
                System.exit(1);
            }
        } catch (final NumberFormatException e) {
            System.err.println("Unrecognized Java version: " + javaVersion);
            System.exit(1);
        }
    }

}
