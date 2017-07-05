package builders.loom.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Stopwatch {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    private static final boolean CPU_TIME_SUPPORTED = THREAD_MX_BEAN.isThreadCpuTimeSupported();
    private static final Map<String, Watch> WATCHES = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> WATCH_NAMES = new ThreadLocal<>();
    private static final AtomicLong TOTAL_DURATION = new AtomicLong();

    private Stopwatch() {
    }

    public static void startProcess(final String name) {
        WATCH_NAMES.set(name);
        if (WATCHES.put(name, new Watch(currentTime())) != null) {
            throw new IllegalStateException("Watch for " + name + " already existed");
        }
    }

    private static long currentTime() {
        return CPU_TIME_SUPPORTED ? THREAD_MX_BEAN.getCurrentThreadCpuTime() : System.nanoTime();
    }

    public static void stopProcess() {
        final String watchName = WATCH_NAMES.get();
        if (watchName == null) {
            throw new IllegalStateException("No watchName registered");
        }
        WATCH_NAMES.remove();

        final Watch watch = WATCHES.get(watchName);
        if (watch == null) {
            throw new IllegalStateException("No watch for " + watchName + " found");
        }
        watch.stop(currentTime());
        TOTAL_DURATION.addAndGet(watch.getDuration());
    }

    public static long getTotalDuration() {
        return TOTAL_DURATION.get();
    }

    public static Map<String, Watch> getWatches() {
        return Collections.unmodifiableMap(WATCHES);
    }

}
