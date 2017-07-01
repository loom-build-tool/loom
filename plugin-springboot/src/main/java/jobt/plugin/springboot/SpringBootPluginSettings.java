package jobt.plugin.springboot;

import jobt.api.PluginSettings;

public class SpringBootPluginSettings implements PluginSettings {

    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

}
