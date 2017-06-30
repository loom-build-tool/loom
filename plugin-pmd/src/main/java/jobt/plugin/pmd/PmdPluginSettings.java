package jobt.plugin.pmd;

import jobt.api.PluginSettings;

public class PmdPluginSettings implements PluginSettings {

    private String minimumPriority = "LOW";

    public String getMinimumPriority() {
        return minimumPriority;
    }

    public void setMinimumPriority(final String minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

}
