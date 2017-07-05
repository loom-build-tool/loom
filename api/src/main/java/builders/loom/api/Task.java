package builders.loom.api;

/**
 * @see AbstractTask
 */
public interface Task {

    TaskStatus run() throws Exception;

}
