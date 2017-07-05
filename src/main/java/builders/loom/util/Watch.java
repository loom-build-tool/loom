package builders.loom.util;

public final class Watch {

    private final long startTime;
    private long duration = -1;

    public Watch(final long startTime) {
        this.startTime = startTime;
    }

    public void stop(final long stopTime) {
        if (duration != -1) {
            throw new IllegalStateException("Watch already stopped");
        }
        duration = stopTime - startTime;
    }

    public long getDuration() {
        return duration;
    }

}
