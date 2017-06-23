package jobt.api;

import java.util.Arrays;
import java.util.HashSet;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin implements Plugin {

    private BuildConfig buildConfig;
    private RuntimeConfiguration runtimeConfiguration;
    private ExecutionContext executionContext;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    @Override
    public void setExecutionContext(final ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public final UsedProducts uses(final String... productIdLists) {
        return new UsedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), getExecutionContext());
    }

    public final ProvidedProducts provides(final String... productIdLists) {
        return new ProvidedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), getExecutionContext());
    }

}
