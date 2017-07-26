/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom;

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
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.util.ThreadUtil;

public class JobPool {

    private static final Logger LOG = LoggerFactory.getLogger(JobPool.class);
    private static final int MONITOR_INTERVAL = 5_000;

    private final AtomicReference<Throwable> firstException = new AtomicReference<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(
        ThreadUtil.newThreadFactory("job-pool"));

    private final Timer timer;
    private final ConcurrentHashMap<String, Job> currentJobs = new ConcurrentHashMap<>();
    private final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

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
            Thread.currentThread().setName("job-" + jobName);

            try {
                currentJobs.put(jobName, job);
                job.call();

                ProgressMonitor.progress();
            } catch (final Throwable e) {
                // In case of any Task error we end up here
                firstException.compareAndSet(null, e);
                if (!(e instanceof InterruptedException)) {
                    LOG.error(e.getMessage(), e);
                }
                executor.shutdownNow();
            } finally {
                currentJobs.remove(jobName);

                // Maybe someone set a new ContextClassLoader -- restore for thread re-use
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }, executor);
    }

    public void shutdown() throws InterruptedException, BuildException {
        LOG.debug("Shutting down Job Pool");
        executor.shutdown();

        LOG.debug("Awaiting Job Pool shutdown");
        executor.awaitTermination(1, TimeUnit.HOURS);
        LOG.debug("Job Pool has shut down");

        timer.cancel();

        if (firstException.get() != null) {
            throw new BuildException(firstException.get());
        }
    }

    public void submitAll(final Collection<Job> jobs) {
        Objects.requireNonNull(jobs, "jobs must not be null");

        jobs.forEach(job -> {
            if (!executor.isShutdown()) {
                submitJob(job);
            }
        });
    }

    private final class MonitorTask extends TimerTask {

        @Override
        public void run() {
            LOG.info("Jobs currently running: {}", currentJobs.values());
        }

    }

}
