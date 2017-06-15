package jobt;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.TaskStatus;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final String name;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final Callable<TaskStatus> callable;

    private List<Job> dependencies;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    Job(final String name, final Callable<TaskStatus> callable) {
        this.name = name;
        this.callable = callable;
    }

    public String getName() {
        return name;
    }

    public JobStatus getStatus() {
        return status.get();
    }

    void setDependencies(final List<Job> dependencies) {
        this.dependencies = dependencies;
    }

    private void waitForCompletion() throws InterruptedException {
        countDownLatch.await();
    }

    @Override
    public TaskStatus call() throws Exception {
        status.set(JobStatus.PREPARING);
        if (!dependencies.isEmpty()) {
            LOG.info("Task {} waits for dependencies {}", name, dependencies);

            status.set(JobStatus.WAITING);
            final long startWait = System.nanoTime();
            for (final Job dependency : dependencies) {
                dependency.waitForCompletion();
            }
            final long stopWait = System.nanoTime();

            final double duration = (stopWait - startWait) / 1_000_000_000D;
            LOG.info(String.format("Execute task %s - dependencies met: %s"
                + " - waited for %.2fs", name, dependencies, duration));
        } else {
            LOG.info("Execute task {} (no dependencies)", name);
        }

        status.set(JobStatus.RUNNING);
        final TaskStatus taskStatus = callable.call();
        countDownLatch.countDown();
        status.set(JobStatus.STOPPED);
        return taskStatus;
    }

    @Override
    public String toString() {
        return "Job{"
            + "name='" + name + '\''
            + ", status=" + status
            + '}';
    }

}
