package jobt.plugin.idea;

import jobt.api.AbstractPlugin;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class IdeaPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        taskRegistry.register("idea", new IdeaTask(getBuildConfig(), getExecutionContext()));
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("idea")
            .dependsOn(
                taskTemplate.task("resolveCompileDependencies"),
                taskTemplate.task("resolveTestDependencies"));
    }

}
