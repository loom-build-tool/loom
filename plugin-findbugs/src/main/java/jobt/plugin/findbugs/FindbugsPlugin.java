package jobt.plugin.findbugs;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;

public class FindbugsPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        task("findbugsMain")
            .impl(() -> new FindbugsTask(getBuildConfig(), CompileTarget.MAIN))
            .provides("findbugsMainReport")
            .uses("source", "compileDependencies", "compilation")
            .register();

        task("findbugsTest")
            .impl(() -> new FindbugsTask(getBuildConfig(), CompileTarget.TEST))
            .provides("findbugsTestReport")
            .uses("testSource", "testDependencies", "compilation", "testCompilation")
            .register();

        goal("check")
            .requires("findbugsMainReport", "findbugsTestReport")
            .register();
    }

}
