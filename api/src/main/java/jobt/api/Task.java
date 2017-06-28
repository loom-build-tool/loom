package jobt.api;

/**
 * <strong>Performance consideration:</strong> A task is registered, if the plugin it's belonging
 * to, is specified in the build configuration &ndash; even if it isn't used in the build process.
 * Ensure quick initialization and put time-consuming code in {@link #run()}.
 *
 * @see jobt.api.AbstractPlugin
 */
public interface Task {

    /**
     * This method is called as soon as all dependencies are met.
     */
    TaskStatus run() throws Exception;

}
