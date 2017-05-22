package jobt.plugin.base;

import jobt.TaskTemplate;
import jobt.config.BuildConfig;
import jobt.plugin.AbstractPlugin;

public class BasePlugin extends AbstractPlugin {

    public BasePlugin(final BuildConfig buildConfig, final TaskTemplate taskTemplate) {
        taskTemplate.task("clean");

        registerTask("clean", new CleanTask());
    }

}
