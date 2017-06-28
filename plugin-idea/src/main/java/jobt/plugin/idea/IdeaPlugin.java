package jobt.plugin.idea;

import jobt.api.AbstractPlugin;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class IdeaPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        taskRegistry.register(
            "configureIdea", new IdeaTask(getBuildConfig()), provides("idea"));
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("configureIdea")
            .uses(
                taskTemplate.product("compileDependencies"),
                taskTemplate.product("testDependencies"));
    }

}
