package jobt;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.Task;
import jobt.api.TaskStatus;
import jobt.util.Stopwatch;

public class Job implements Callable<TaskStatus> {

    private static final Logger LOG = LoggerFactory.getLogger(Job.class);

    private final String name;
    private final AtomicReference<JobStatus> status = new AtomicReference<>(JobStatus.INITIALIZING);
    private final Task task;

    private List<Job> dependencies = Collections.emptyList();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    Job(final String name, final Task task) {
        this.name = name;
        this.task = task;
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

        prepareTask();

        waitForDependencies();

        status.set(JobStatus.RUNNING);
        final TaskStatus taskStatus = runTask();
        countDownLatch.countDown();
        status.set(JobStatus.STOPPED);
        return taskStatus;
    }

    private void prepareTask() throws Exception {
        LOG.info("Prepare task {}", name);

        Stopwatch.startProcess("Task " + name + " > init");
        task.prepare();
        Stopwatch.stopProcess();

        LOG.info("Prepared task {}", name);
    }

    private void waitForDependencies() throws InterruptedException {
        if (dependencies.isEmpty()) {
            LOG.info("Execute task {} (no dependencies)", name);
            return;
        }

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
    }

    public TaskStatus runTask() throws Exception {
        LOG.info("Start task {}", name);

        Stopwatch.startProcess("Task " + name + " > run");
        final TaskStatus taskStatus = task.run();
        Stopwatch.stopProcess();

        LOG.info("Task {} resulted with {}", name, taskStatus);
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
