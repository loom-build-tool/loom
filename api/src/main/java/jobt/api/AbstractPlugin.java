package jobt.api;

import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractPlugin implements Plugin {

    protected BuildConfig buildConfig;
    protected ExecutionContext executionContext;
    protected DependencyResolver dependencyResolver;
    protected Logger logger;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public void setExecutionContext(final ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public void setDependencyResolver(final DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    @Override
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<String> dependencies() {
        return null;
    }

}
