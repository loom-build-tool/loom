package jobt.plugin.findbugs;

import java.util.Optional;

import jobt.api.PluginSettings;

public class FindbugsPluginSettings implements PluginSettings {

    private String priorityThreshold;
    private String customPlugins;

    public Optional<String> getPriorityThreshold() {
        return Optional.ofNullable(priorityThreshold);
    }

    public void setPriorityThreshold(final String priorityThreshold) {
        this.priorityThreshold = priorityThreshold;
    }

    public String getCustomPlugins() {
        return customPlugins;
    }

    public void setCustomPlugins(final String customPlugins) {
        this.customPlugins = customPlugins;
    }

}
