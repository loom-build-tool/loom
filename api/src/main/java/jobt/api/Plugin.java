package jobt.api;

/**
 * <strong>Performance consideration:</strong> A plugin is registered, if specified in the build
 * configuration &ndash; even if it isn't used in the build process. Ensure quick initialization and
 * put time-consuming code in {@link Task#run()}.
 *
 * @see jobt.api.AbstractPlugin
 */
public interface Plugin {

    void setBuildConfig(BuildConfig buildConfig);

    void setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration);

    void setProductRepository(ProductRepository productRepository);

    void configure(TaskRegistry taskRegistry);

    void configure(TaskTemplate taskTemplate);

}
