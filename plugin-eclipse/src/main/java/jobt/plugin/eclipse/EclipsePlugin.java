package jobt.plugin.eclipse;

import jobt.api.AbstractPlugin;
import jobt.api.PluginSettings;

public class EclipsePlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("configureEclipse")
            .impl(() -> new EclipseTask(getBuildConfig()))
            .provides("eclipse")
            .uses("testDependencies")
            .register();
    }

}
