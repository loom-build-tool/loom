package jobt.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public final class Watch {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    public static final boolean CPU_TIME_SUPPORTED = THREAD_MX_BEAN.isThreadCpuTimeSupported();

    private final long threadId = Thread.currentThread().getId();
    private final long startTime;
    private long duration = -1;

    public Watch() {
        if (CPU_TIME_SUPPORTED) {
            startTime = THREAD_MX_BEAN.getThreadCpuTime(threadId);
        } else {
            startTime = System.nanoTime();
        }
    }

    public void stop() {
        Preconditions.checkState(
            threadId == Thread.currentThread().getId(), "Watch must not span multiple threads");

        if (CPU_TIME_SUPPORTED) {
            duration = THREAD_MX_BEAN.getThreadCpuTime(threadId) - startTime;
        } else {
            duration = System.nanoTime() - startTime;
        }

    }

    public long getDuration() {
        return duration;
    }

}
