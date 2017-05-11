package jobt;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Stopwatch {

    private Map<String, Watch> watches = new ConcurrentHashMap<>();
    private AtomicLong totalDuration = new AtomicLong();

    public void startProcess(final String name) {
        final Watch put = watches.put(name, new Watch());
        if (put != null) {
            throw new IllegalStateException("Watch for " + name + " already existed");
        }
    }

    public void stopProcess(final String name) {
        final Watch watch = watches.get(name);
        watch.stop();
        totalDuration.addAndGet(watch.getDuration());
    }

    public AtomicLong getTotalDuration() {
        return totalDuration;
    }

    public Map<String, Watch> getWatches() {
        return Collections.unmodifiableMap(watches);
    }

}
