package jobt.api;

/**
 * <strong>Performance consideration:</strong> A task is registered, if the plugin it's belonging
 * to, is specified in the build configuration &ndash; even if it isn't used in the build process.
 * Ensure quick initialization and put time-consuming code in {@link #prepare()} and {@link #run()}.
 *
 * @see jobt.api.AbstractPlugin
 */
public interface Task extends ProductDependenciesAware {

    /**
     * Prepare for run. This method might be called before the task dependencies are met.
     * The prepare method of other tasks might be called in parallel.
     */
    void prepare() throws Exception;

    /**
     * This method is called as soon as all dependencies are met.
     */
    TaskStatus run() throws Exception;

}
