package jobt.plugin.java;

import java.util.Optional;

import jobt.api.PluginSettings;

public class JavaPluginSettings implements PluginSettings {

    private String mainClassName;

    public Optional<String> getMainClassName() {
        return Optional.ofNullable(mainClassName);
    }

    public void setMainClassName(final String mainClassName) {
        this.mainClassName = mainClassName;
    }

}
