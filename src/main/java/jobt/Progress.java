package jobt;

import java.util.Collections;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class Progress {

    private static final double NANO_TO_SEC = 1_000_000_000D;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private static long lastTimestamp;
    private static String lastMessage;

    private Progress() {
    }

    public static void newStatus(final String message) {
        lastTimestamp = System.nanoTime();
        lastMessage = message;
        System.out.print(message);
    }

    public static void ok() {
        complete("OK", ANSI_GREEN);
    }

    public static void skip() {
        complete("SKIPPED", ANSI_WHITE);
    }

    public static void uptodate() {
        complete("UP TO DATE", ANSI_CYAN);
    }

    private static void complete(final String status, final String color) {
        final long duration = System.nanoTime() - lastTimestamp;

        final int maxMsgLength = 35;
        final String space = String.join("",
            Collections.nCopies(maxMsgLength - lastMessage.length(), " "));

        System.out.printf(space + " [" + color + "%10s" + ANSI_RESET + "] "
            + "[+ %.3fs]%n", status, duration / NANO_TO_SEC);
    }

    public static void log(final String message, final Object... args) {
        if (args == null) {
            System.out.println(message);
        } else {
            System.out.println(String.format(message, args));
        }
    }

}
