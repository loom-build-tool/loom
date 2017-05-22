package jobt;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class Progress {

    private static final double NANO_TO_SEC = 1_000_000_000D;

    private static long lastTimestamp;

    private Progress() {
    }

    public static void newStatus(final String message) {
        lastTimestamp = System.nanoTime();
        System.out.print(message);
    }

    public static void complete() {
        complete("OK");
    }

    public static void complete(final String status) {
        final long duration = System.nanoTime() - lastTimestamp;
        System.out.printf(" [%s] [+ %.3fs]%n", status, duration / NANO_TO_SEC);
    }

    public static void log(final String message, final Object... args) {
        if (args == null) {
            System.out.println(message);
        } else {
            System.out.println(String.format(message, args));
        }
    }

}
