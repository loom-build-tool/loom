package jobt.plugin;

import java.util.List;
import java.util.logging.Logger;

public interface Plugin {

    void setBuildConfig(BuildConfig buildConfig);

    void setExecutionContext(ExecutionContext executionContext);

    void setDependencyResolver(DependencyResolver dependencyResolver);

    void setLogger(Logger logger);

    List<String> dependencies();

    void configure(TaskRegistry taskRegistry);

    void configure(TaskTemplate taskTemplate);

}
