package builders.loom.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import builders.loom.api.Project;
import builders.loom.api.BuildSettings;

class BuildConfigImpl implements BuildConfigWithSettings, Serializable {

    private static final long serialVersionUID = 1L;

    private final Project project;
    private final Set<String> plugins;
    private final BuildSettings buildSettings;
    private final Map<String, String> settings;
    private final Set<String> dependencies;
    private final Set<String> testDependencies;

    BuildConfigImpl(final ProjectImpl project, final Set<String> plugins,
                    final BuildSettings buildSettings,
                    final Map<String, String> settings,
                    final Set<String> dependencies, final Set<String> testDependencies) {
        this.project = Objects.requireNonNull(project);
        this.plugins = Collections.unmodifiableSet(Objects.requireNonNull(plugins));
        this.buildSettings = Objects.requireNonNull(buildSettings);
        this.settings = Collections.unmodifiableMap(Objects.requireNonNull(settings));
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
    public BuildSettings getBuildSettings() {
        return buildSettings;
    }

    @Override
    public Map<String, String> getSettings() {
        return settings;
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
            + ", buildSettings=" + buildSettings
            + ", dependencies=" + dependencies
            + ", testDependencies=" + testDependencies
            + '}';
    }

}
