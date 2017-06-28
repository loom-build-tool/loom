package jobt.plugin.findbugs;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;

public class FindbugsPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        task("findbugsMain")
            .impl(() -> new FindbugsTask(getBuildConfig(), CompileTarget.MAIN))
            .uses("source", "compileDependencies", "compilation")
            .provides("findbugsMainReport")
            .register();

        task("findbugsTest")
            .impl(() -> new FindbugsTask(getBuildConfig(), CompileTarget.TEST))
            .uses("testSource", "testDependencies", "compilation", "testCompilation")
            .provides("findbugsTestReport")
            .register();

        goal("check")
            .requires("findbugsMainReport", "findbugsTestReport")
            .register();
    }

}
