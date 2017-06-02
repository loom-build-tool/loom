package jobt.plugin;

import java.util.List;

public abstract class AbstractPlugin implements Plugin {

    protected BuildConfig buildConfig;
    protected ExecutionContext executionContext;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public void setExecutionContext(final ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public List<String> dependencies() {
        return null;
    }

}
