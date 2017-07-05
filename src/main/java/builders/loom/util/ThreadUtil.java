package builders.loom.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtil {

    private ThreadUtil() {
    }

    public static ThreadFactory newThreadFactory(final String namePrefix) {
        return new ThreadFactory() {
            private final AtomicInteger threadId = new AtomicInteger(1);

            @Override
            public Thread newThread(final Runnable r) {
                final Thread thread = new Thread(r, namePrefix + "-" + threadId.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
    }

}
