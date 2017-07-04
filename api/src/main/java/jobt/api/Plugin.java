package jobt.api;

import jobt.api.service.ServiceLocatorRegistration;

/**
 * <strong>Performance consideration:</strong> A plugin is registered, if specified in the build
 * configuration &ndash; even if it isn't used in the build process. Ensure quick initialization and
 * put time-consuming code in the {@link Task}.
 *
 * @see jobt.api.AbstractPlugin
 */
public interface Plugin {

    PluginSettings getPluginSettings();

    void setTaskRegistry(TaskRegistry taskRegistry);

    void setServiceLocator(ServiceLocatorRegistration serviceLocator);

    void setBuildConfig(BuildConfig buildConfig);

    void setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration);

    void configure();

}
