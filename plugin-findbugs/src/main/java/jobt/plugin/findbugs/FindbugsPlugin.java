package jobt.plugin.findbugs;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;

public class FindbugsPlugin extends AbstractPlugin {

    @Override
    public FindbugsPluginSettings getPluginSettings() {
        return new FindbugsPluginSettings();
    }

    @Override
    public void configure() {
        final FindbugsPluginSettings pluginConfiguration = getPluginSettings();

        task("findbugsMain")
            .impl(() -> new FindbugsTask(getBuildConfig(), pluginConfiguration, CompileTarget.MAIN))
            .provides("findbugsMainReport")
            .uses("source", "compileDependencies", "compilation")
            .register();

        task("findbugsTest")
            .impl(() -> new FindbugsTask(getBuildConfig(), pluginConfiguration, CompileTarget.TEST))
            .provides("findbugsTestReport")
            .uses("testSource", "testDependencies", "compilation", "testCompilation")
            .register();

        goal("check")
            .requires("findbugsMainReport", "findbugsTestReport")
            .register();
    }

}
