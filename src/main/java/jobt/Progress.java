package jobt;

public class Progress {

    public static void newStatus(final String message) {
        System.out.print(message);
    }

    public static void complete() {
        System.out.println(" [OK]");
    }

    public static void log(final String message) {
        System.out.println(message);
    }
}
