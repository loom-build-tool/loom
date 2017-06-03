package jobt.api;

import java.util.List;

public interface Plugin {

    void setBuildConfig(BuildConfig buildConfig);

    void setExecutionContext(ExecutionContext executionContext);

    void setDependencyResolver(DependencyResolver dependencyResolver);

    List<String> dependencies();

    void configure(TaskRegistry taskRegistry);

    void configure(TaskTemplate taskTemplate);

}
