package jobt.plugin;

import java.util.List;

public interface Plugin {

    void setBuildConfig(BuildConfig buildConfig);

    void setExecutionContext(ExecutionContext executionContext);

    List<String> dependencies();

    void configure(TaskRegistry taskRegistry);

    void configure(TaskTemplate taskTemplate);

}
