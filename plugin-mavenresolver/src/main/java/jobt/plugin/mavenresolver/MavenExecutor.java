package jobt.plugin.mavenresolver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME move executor close to resolver
public final class MavenExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(MavenExecutor.class);

    private static final ExecutorService POOL;

    private MavenExecutor() {
    }

    static {

        // execute maven resolution in a streamlined manner
        POOL = Executors.newSingleThreadExecutor(
            r -> {
                final Thread thread = new Thread(r);
                thread.setName("thread-" + MavenExecutor.class.getSimpleName());
                thread.setDaemon(true);
                return thread;
            });
    }

    public static <T> Future<T> submit(final Callable<T> callable) {
        return POOL.submit(callable);
    }

}
