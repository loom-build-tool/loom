package jobt.plugin.pmd;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;

public class PmdPlugin extends AbstractPlugin<PmdPluginSettings> {

    public PmdPlugin() {
        super(new PmdPluginSettings());
    }

    @Override
    public void configure() {
        task("pmdMain")
            .impl(() -> new PmdTask(getBuildConfig(), getPluginSettings(), CompileTarget.MAIN))
            .provides("pmdMainReport")
            .uses("source", "compileDependencies")
            .register();

        task("pmdTest")
            .impl(() -> new PmdTask(getBuildConfig(), getPluginSettings(), CompileTarget.TEST))
            .provides("pmdTestReport")
            .uses("testSource", "testDependencies")
            .register();

        goal("check")
            .requires("pmdMainReport", "pmdTestReport")
            .register();
    }

}
