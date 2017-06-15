package jobt;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.TaskStatus;

public class JobPool {

    private static final Logger LOG = LoggerFactory.getLogger(JobPool.class);
    private static final int MONITOR_INTERVAL = 5_000;

    private final AtomicReference<Throwable> firstException = new AtomicReference<>();
    private final ExecutorService executor = Executors.newWorkStealingPool();
    private final Timer timer;
    private final ConcurrentHashMap<String, Job> currentJobs = new ConcurrentHashMap<>();

    public JobPool() {
        timer = new Timer("JobPoolMonitor", true);
        timer.schedule(new MonitorTask(), MONITOR_INTERVAL, MONITOR_INTERVAL);
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    public void submitJob(final Job job) {
        if (executor.isShutdown()) {
            throw new IllegalStateException("Pool already shut down");
        }

        LOG.debug("Submit job {}", job.getName());

        CompletableFuture.runAsync(() -> {
            LOG.info("Start job {}", job.getName());
            try {
                currentJobs.put(job.getName(), job);
                final TaskStatus status = job.call();
                LOG.info("Job {} resulted with {}", job.getName(), status);
            } catch (final Throwable e) {
                firstException.compareAndSet(null, e);
                if (!(e instanceof InterruptedException)) {
                    LOG.error(e.getMessage(), e);
                }
                executor.shutdownNow();
            } finally {
                currentJobs.remove(job.getName());
            }
        }, executor);
    }

    public void shutdown() throws InterruptedException {
        LOG.debug("Shutting down Job Pool");
        executor.shutdown();

        LOG.debug("Awaiting Job Pool shutdown");
        executor.awaitTermination(1, TimeUnit.HOURS);
        LOG.debug("Job Pool has shut down");

        timer.cancel();

        if (firstException.get() != null) {
            throw new IllegalStateException(firstException.get());
        }
    }

    private final class MonitorTask extends TimerTask {

        @Override
        public void run() {
            LOG.info("Jobs currently running: {}", currentJobs.values());
        }

    }

}
