package builders.loom.plugin.eclipse;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;

public class EclipsePlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("configureEclipse")
            .impl(() -> new EclipseTask(getBuildConfig()))
            .provides("eclipse")
            .uses("testArtifacts")
            .register();
    }

}
