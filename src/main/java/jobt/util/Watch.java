package jobt.util;

public class Watch {

    private long startTime = System.nanoTime();
    private long duration;

    public void stop() {
        duration = System.nanoTime() - startTime;
    }

    public long getDuration() {
        return duration;
    }

}
