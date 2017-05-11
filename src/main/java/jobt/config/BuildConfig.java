package jobt.config;

import java.util.List;
import java.util.Map;

public class BuildConfig {

    private Project project;
    private List<String> plugins;
    private Map<String, String> configuration;
    private List<String> dependencies;
    private List<String> testDependencies;

    public Project getProject() {
        return project;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(final List<String> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final List<String> testDependencies) {
        this.testDependencies = testDependencies;
    }

}
