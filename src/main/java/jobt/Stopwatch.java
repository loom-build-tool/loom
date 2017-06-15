package jobt;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Stopwatch {

    private static final Map<String, Watch> WATCHES = new ConcurrentHashMap<>();
    private static final AtomicLong TOTAL_DURATION = new AtomicLong();

    private Stopwatch() {
    }

    public static void startProcess(final String name) {
        final Watch put = WATCHES.put(name, new Watch());
        if (put != null) {
            throw new IllegalStateException("Watch for " + name + " already existed");
        }
    }

    public static void stopProcess(final String name) {
        final Watch watch = WATCHES.get(name);
        watch.stop();
        TOTAL_DURATION.addAndGet(watch.getDuration());
    }

    public static long getTotalDuration() {
        return TOTAL_DURATION.get();
    }

    public static Map<String, Watch> getWatches() {
        return Collections.unmodifiableMap(WATCHES);
    }

}
