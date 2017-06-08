package jobt.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jobt.api.BuildConfig;

public class BuildConfigImpl implements BuildConfig, Serializable {

    private static final long serialVersionUID = 1L;

    private ProjectImpl project;
    private Set<String> plugins = Collections.emptySet();
    private Map<String, String> configuration = Collections.emptyMap();
    private Set<String> dependencies = Collections.emptyNavigableSet();
    private Set<String> testDependencies = Collections.emptyNavigableSet();

    @Override
    public ProjectImpl getProject() {
        return project;
    }

    public void setProject(final ProjectImpl project) {
        this.project = project;
    }

    @Override
    public Set<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(final Set<String> plugins) {
        this.plugins = Collections.unmodifiableSet(plugins);
    }

    @Override
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = Collections.unmodifiableMap(configuration);
    }

    @Override
    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final Set<String> dependencies) {
        this.dependencies = Collections.unmodifiableSet(dependencies);
    }

    @Override
    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final Set<String> testDependencies) {
        this.testDependencies = Collections.unmodifiableSet(testDependencies);
    }

}
