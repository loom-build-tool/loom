package builders.loom.plugin.junit4;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;

public class Junit4Plugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("runTest")
            .impl(Junit4TestTask::new)
            .provides("test")
            .uses("testDependencies", "processedResources", "compilation",
                "processedTestResources", "testCompilation")
            .register();

        goal("check")
            .requires("test")
            .register();
    }

}
