package jobt;

public class Jobt {

    public static void main(final String[] args) {
        final long startTime = System.nanoTime();
        System.out.println("Java Optimized Build Tool v1.0.0");

        final ProcessMonitor processMonitor = new ProcessMonitor();
        try {
            new CliProcessor(processMonitor, args).run();

            processMonitor.stop();
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.err.println("Build failed - " + e.getLocalizedMessage());
            System.exit(1);
        }

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        processMonitor.endProcess(String.format("✨ Built in %.2fs%n", duration));
    }

}
