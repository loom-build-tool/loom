package jobt.config;

import java.util.List;
import java.util.Map;

import jobt.plugin.BuildConfig;

public class BuildConfigImpl implements BuildConfig {

    private ProjectImpl project;
    private List<String> plugins;
    private Map<String, String> configuration;
    private List<String> dependencies;
    private List<String> testDependencies;

    @Override
    public ProjectImpl getProject() {
        return project;
    }

    public void setProject(final ProjectImpl project) {
        this.project = project;
    }

    @Override
    public List<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(final List<String> plugins) {
        this.plugins = plugins;
    }

    @Override
    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Map<String, String> configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final List<String> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public List<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final List<String> testDependencies) {
        this.testDependencies = testDependencies;
    }

}
