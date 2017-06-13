package jobt.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobtExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(JobtExecutor.class);

    private JobtExecutor() {
    }

    private static final ExecutorService POOL;

    static {

        final int cores = dedicatedCores();

        LOG.info("Configure thread pool with {} cores", cores);

        POOL = Executors.newFixedThreadPool(
            cores, r -> {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
    }

    private static int dedicatedCores() {
        return Math.max(
            (Runtime.getRuntime().availableProcessors() - 1) / 2,
            2);
    }

    public static void submit(final Callable<?> callable) {
        POOL.submit(callable);
    }

    public static <T> Future<T> call(final Callable<T> callable) {
        return POOL.submit(callable);
    }

}
