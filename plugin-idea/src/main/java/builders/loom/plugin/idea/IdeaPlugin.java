package builders.loom.plugin.idea;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;

public class IdeaPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("configureIdea")
            .impl(() -> new IdeaTask(getBuildConfig()))
            .provides("idea")
            .uses("compileArtifacts", "testArtifacts")
            .register();
    }

}
