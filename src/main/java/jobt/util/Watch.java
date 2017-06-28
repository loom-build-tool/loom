package jobt.util;

public class Watch {

    private final long startTime = System.nanoTime();
    private long duration;

    public void stop() {
        duration = System.nanoTime() - startTime;
    }

    public long getDuration() {
        return duration;
    }

}
