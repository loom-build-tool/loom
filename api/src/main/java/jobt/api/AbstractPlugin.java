package jobt.api;

import java.util.List;
import java.util.logging.Logger;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin implements Plugin {

    private BuildConfig buildConfig;
    private ExecutionContext executionContext;
    private DependencyResolver dependencyResolver;
    private Logger logger;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public void setExecutionContext(final ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Override
    public void setDependencyResolver(final DependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public DependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    @Override
    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    @Override
    public List<String> dependencies() {
        return null;
    }

}
