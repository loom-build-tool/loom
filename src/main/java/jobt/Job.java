package jobt;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
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

    Job(final String name, final Task task) {
        this.name = Objects.requireNonNull(name, "name required");
        this.task = Objects.requireNonNull(task, "task required");
    }

    public String getName() {
        return name;
    }

    public JobStatus getStatus() {
        return status.get();
    }

    void setDependencies(final List<Job> dependencies) {
        // FIXME remove
    }

    @Override
    public TaskStatus call() throws Exception {
        status.set(JobStatus.PREPARING);

        prepareTask();

        status.set(JobStatus.RUNNING);
        final TaskStatus taskStatus = runTask();

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
