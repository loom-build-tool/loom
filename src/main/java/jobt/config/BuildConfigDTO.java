package jobt.config;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class BuildConfigDTO {

    private ProjectDTO project;
    private Set<String> plugins;
    private Map<String, String> configuration;
    private Set<String> dependencies;
    private Set<String> testDependencies;

    public ProjectDTO getProject() {
        return project;
    }

    public void setProject(final ProjectDTO project) {
        this.project = project;
    }

    public Set<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(final Set<String> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final Set<String> testDependencies) {
        this.testDependencies = testDependencies;
    }

    public BuildConfigImpl build() {
        return new BuildConfigImpl(
            project.build(),
            plugins != null ? plugins : Collections.emptySet(),
            configuration != null ? configuration : Collections.emptyMap(),
            dependencies != null ? dependencies : Collections.emptySet(),
            testDependencies != null ? testDependencies : Collections.emptySet());
    }

}
