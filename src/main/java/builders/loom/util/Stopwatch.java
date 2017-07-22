package builders.loom.util;

public final class Stopwatch {

    private final long start = System.nanoTime();

    public String duration() {
        final double duration = (System.nanoTime() - start) / 1_000_000D;
        return String.format("%.2f", duration);
    }

}
