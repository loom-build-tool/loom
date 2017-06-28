package jobt.plugin.idea;

import jobt.api.AbstractPlugin;

public class IdeaPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        task("configureIdea")
            .impl(() -> new IdeaTask(getBuildConfig()))
            .provides("idea")
            .uses("compileDependencies", "testDependencies")
            .register();
    }

}
