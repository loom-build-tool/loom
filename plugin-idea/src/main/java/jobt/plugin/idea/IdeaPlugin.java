package jobt.plugin.idea;

import jobt.api.AbstractPlugin;
import jobt.api.PluginSettings;

public class IdeaPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("configureIdea")
            .impl(() -> new IdeaTask(getBuildConfig()))
            .provides("idea")
            .uses("compileDependencies", "testDependencies")
            .register();
    }

}
