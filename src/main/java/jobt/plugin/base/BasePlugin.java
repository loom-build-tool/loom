package jobt.plugin.base;

import jobt.api.AbstractPlugin;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class BasePlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        taskRegistry.register("clean", new CleanTask());
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("clean");
    }

}
