package jobt.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jobt.config.BuildConfig;

public class PluginRegistry {

    private final List<Plugin> plugins = new ArrayList<>();

    public PluginRegistry(final BuildConfig buildConfig) {
        for (final String plugin : buildConfig.getPlugins()) {
            switch (plugin) {
                case "java":
                    this.plugins.add(new JavaPlugin(buildConfig));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown plugin: " + plugin);
            }
        }
    }

    public void trigger(final String phase) throws Exception {
        for (final Plugin plugin : plugins) {
            if (phase.equals(plugin.getRegisteredPhase())) {
                plugin.run();
            }
        }
    }

    public List<Plugin> getPhasePlugins(final String phase) {
        return plugins.stream()
            .filter(p -> p.getRegisteredPhase().equals(phase))
            .collect(Collectors.toList());
    }
}
