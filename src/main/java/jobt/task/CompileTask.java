package jobt.task;

import jobt.plugin.Plugin;
import jobt.plugin.PluginRegistry;

public class CompileTask implements Task {

    private final PluginRegistry pluginRegistry;

    public CompileTask(final PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void run() throws Exception {
        for (final Plugin plugin : pluginRegistry.getPhasePlugins("compile")) {
            plugin.run();
        }
    }

}
