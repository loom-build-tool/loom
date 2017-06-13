package jobt.api;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JobtExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(JobtExecutor.class);

    // in seconds
    private static final int FUTURE_MAX_WAITTIME = 30;

    private static final ExecutorService POOL;

    private JobtExecutor() {
    }

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

    public static <T> Future<T> submit(final Callable<T> callable) {
        return POOL.submit(callable);
    }

    public static <T> T waitAndGet(final Future<T> future) {
        try {
            return future.get(FUTURE_MAX_WAITTIME, TimeUnit.SECONDS);
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {
            throw new IllegalStateException(e);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
