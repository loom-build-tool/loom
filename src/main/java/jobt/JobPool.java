package jobt;

import java.util.Collection;
import java.util.Objects;
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
import jobt.util.ThreadUtil;

public class JobPool {

    private static final Logger LOG = LoggerFactory.getLogger(JobPool.class);
    private static final int MONITOR_INTERVAL = 5_000;

    private final AtomicReference<Throwable> firstException = new AtomicReference<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(
        ThreadUtil.newThreadFactory("job-pool"));

    private final Timer timer;
    private final ConcurrentHashMap<String, Job> currentJobs = new ConcurrentHashMap<>();

    public JobPool() {
        timer = new Timer("JobPoolMonitor", true);
        timer.schedule(new MonitorTask(), MONITOR_INTERVAL, MONITOR_INTERVAL);
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    public void submitJob(final Job job) {
        Objects.requireNonNull(job, "job must not be null");

        if (executor.isShutdown()) {
            throw new IllegalStateException("Pool already shut down");
        }

        final String jobName = job.getName();
        LOG.debug("Submit job {}", jobName);

        CompletableFuture.runAsync(() -> {
            LOG.info("Start job {}", jobName);
            try {
                currentJobs.put(jobName, job);
                final TaskStatus status = job.call();
                LOG.info("Job {} resulted with {}", jobName, status);
            } catch (final Throwable e) {
                firstException.compareAndSet(null, e);
                if (!(e instanceof InterruptedException)) {
                    LOG.error(e.getMessage(), e);
                }
                executor.shutdownNow();
            } finally {
                currentJobs.remove(jobName);
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

    public void submitAll(final Collection<Job> jobs) {
        Objects.requireNonNull(jobs, "jobs must not be null");
        for (final Job job : jobs) {
            submitJob(job);
        }
    }

    private final class MonitorTask extends TimerTask {

        @Override
        public void run() {
            LOG.info("Jobs currently running: {}", currentJobs.values());
        }

    }

}
