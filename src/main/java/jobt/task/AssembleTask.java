package jobt.task;

import jobt.plugin.Plugin;
import jobt.plugin.PluginRegistry;

public class AssembleTask implements Task {

    private final PluginRegistry pluginRegistry;

    public AssembleTask(final PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public void run() throws Exception {
        for (final Plugin plugin : pluginRegistry.getPhasePlugins("assemble")) {
            plugin.run();
        }
    }

}
