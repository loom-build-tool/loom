package jobt.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jobt.api.BuildConfig;
import jobt.api.Project;

public class BuildConfigImpl implements BuildConfig, Serializable {

    private static final long serialVersionUID = 1L;

    private Project project;
    private Set<String> plugins;
    private Map<String, String> configuration;
    private Set<String> dependencies;
    private Set<String> testDependencies;

    public BuildConfigImpl(final ProjectImpl project, final Set<String> plugins,
                           final Map<String, String> configuration,
                           final Set<String> dependencies, final Set<String> testDependencies) {
        this.project = Objects.requireNonNull(project);
        this.plugins = Collections.unmodifiableSet(Objects.requireNonNull(plugins));
        this.configuration = Collections.unmodifiableMap(Objects.requireNonNull(configuration));
        this.dependencies = Collections.unmodifiableSet(Objects.requireNonNull(dependencies));
        this.testDependencies =
            Collections.unmodifiableSet(Objects.requireNonNull(testDependencies));
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Set<String> getPlugins() {
        return plugins;
    }

    @Override
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    @Override
    public Set<String> getDependencies() {
        return dependencies;
    }

    @Override
    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    @Override
    public String toString() {
        return "BuildConfigImpl{"
            + "project=" + project
            + ", plugins=" + plugins
            + ", configuration=" + configuration
            + ", dependencies=" + dependencies
            + ", testDependencies=" + testDependencies
            + '}';
    }

}
