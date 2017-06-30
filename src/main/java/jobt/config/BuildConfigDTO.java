package jobt.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jobt.api.JavaVersion;

public class BuildConfigDTO {

    private static final JavaVersion DEFAULT_JAVA_PLATFORM_VERSION = JavaVersion.JAVA_1_8;

    private ProjectDTO project;
    private Set<String> plugins;
    private Map<String, String> settings;
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

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(final Map<String, String> settings) {
        this.settings = settings;
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
        final Map<String, String> cfg = settings != null ? settings : new HashMap<>();

        final String javaPlatformVersionStr = cfg.remove("javaPlatformVersion");
        final JavaVersion javaPlatformVersion = javaPlatformVersionStr != null
            ? JavaVersion.ofVersion(javaPlatformVersionStr)
            : DEFAULT_JAVA_PLATFORM_VERSION;

        final BuildSettingsImpl buildSettings = new BuildSettingsImpl(javaPlatformVersion);

        return new BuildConfigImpl(
            project.build(),
            plugins != null ? plugins : Collections.emptySet(),
            buildSettings,
            cfg,
            dependencies != null ? dependencies : Collections.emptySet(),
            testDependencies != null ? testDependencies : Collections.emptySet());
    }

}
