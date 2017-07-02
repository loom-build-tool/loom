package jobt.plugin.pmd;

import jobt.api.PluginSettings;

public class PmdPluginSettings implements PluginSettings {

    private String minimumPriority = "LOW";
    private String ruleSets = "rulesets/java/basic.xml";

    public String getMinimumPriority() {
        return minimumPriority;
    }

    public void setMinimumPriority(final String minimumPriority) {
        this.minimumPriority = minimumPriority;
    }

    public String getRuleSets() {
        return ruleSets;
    }

    public void setRuleSets(final String ruleSets) {
        this.ruleSets = ruleSets;
    }

}
