package jobt;

@SuppressWarnings("checkstyle:regexpmultiline")
public final class Progress {

    private Progress() {
    }

    public static void newStatus(final String message) {
        System.out.print(message);
    }

    public static void complete() {
        complete("OK");
    }

    public static void complete(final String status) {
        System.out.println(" [" + status + "]");
    }

    public static void log(final String message) {
        System.out.println(message);
    }

}
