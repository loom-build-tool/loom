package jobt.api;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin implements Plugin {

    private BuildConfig buildConfig;
    private ClassLoader extClassLoader;
    private ExecutionContext executionContext;
    private DependencyResolver dependencyResolver;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public void setExtClassLoader(final ClassLoader extClassLoader) {
        this.extClassLoader = extClassLoader;
    }

    public ClassLoader getExtClassLoader() {
        return extClassLoader;
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

}
