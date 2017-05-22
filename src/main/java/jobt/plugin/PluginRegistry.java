package jobt.plugin;

import java.util.ArrayList;
import java.util.List;

import jobt.TaskTemplate;
import jobt.config.BuildConfig;
import jobt.plugin.base.BasePlugin;
import jobt.plugin.java.JavaPlugin;

public class PluginRegistry {

    private final List<Plugin> plugins = new ArrayList<>();

    public PluginRegistry(final BuildConfig buildConfig, final TaskTemplate taskTemplate) {
        plugins.add(new BasePlugin(buildConfig, taskTemplate));

        for (final String plugin : buildConfig.getPlugins()) {
            switch (plugin) {
                case "java":
                    plugins.add(new JavaPlugin(buildConfig, taskTemplate));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown plugin: " + plugin);
            }
        }
    }

    public void trigger(final String phase) throws Exception {
        for (final Plugin plugin : plugins) {
            plugin.run(phase);
        }
    }

}
