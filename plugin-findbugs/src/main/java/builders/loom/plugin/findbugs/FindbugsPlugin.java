package builders.loom.plugin.findbugs;

import builders.loom.api.CompileTarget;
import builders.loom.api.AbstractPlugin;

public class FindbugsPlugin extends AbstractPlugin<FindbugsPluginSettings> {

    public FindbugsPlugin() {
        super(new FindbugsPluginSettings());
    }

    @Override
    public void configure() {
        final FindbugsPluginSettings pluginSettings = getPluginSettings();

        task("findbugsMain")
            .impl(() -> new FindbugsTask(pluginSettings, CompileTarget.MAIN))
            .provides("findbugsMainReport")
            .uses("source", "compileDependencies", "compilation")
            .register();

        task("findbugsTest")
            .impl(() -> new FindbugsTask(pluginSettings, CompileTarget.TEST))
            .provides("findbugsTestReport")
            .uses("testSource", "testDependencies", "compilation", "testCompilation")
            .register();

        goal("check")
            .requires("findbugsMainReport", "findbugsTestReport")
            .register();
    }

}
