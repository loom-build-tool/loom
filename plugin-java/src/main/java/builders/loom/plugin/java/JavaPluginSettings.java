package builders.loom.plugin.java;

import java.util.Optional;

import builders.loom.api.PluginSettings;

public class JavaPluginSettings implements PluginSettings {

    private String mainClassName;
    private String resourceFilterGlob;

    public Optional<String> getMainClassName() {
        return Optional.ofNullable(mainClassName);
    }

    public void setMainClassName(final String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public String getResourceFilterGlob() {
        return resourceFilterGlob;
    }

    public void setResourceFilterGlob(final String resourceFilterGlob) {
        this.resourceFilterGlob = resourceFilterGlob;
    }

}
