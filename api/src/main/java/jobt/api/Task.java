package jobt.api;

/**
 * @see jobt.api.AbstractTask
 */
public interface Task {

    TaskStatus run() throws Exception;

}
