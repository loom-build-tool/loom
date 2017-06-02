package jobt.plugin.base;

import jobt.plugin.AbstractPlugin;
import jobt.plugin.TaskRegistry;
import jobt.plugin.TaskTemplate;

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
