package jobt.plugin.checkstyle;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.PluginSettings;

public class CheckstylePlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("checkstyleMain")
            .impl(() -> new CheckstyleTask(CompileTarget.MAIN))
            .provides("checkstyleMainReport")
            .uses("source", "compileDependencies")
            .register();

        task("checkstyleTest")
            .impl(() -> new CheckstyleTask(CompileTarget.TEST))
            .provides("checkstyleTestReport")
            .uses("testSource", "testDependencies")
            .register();

        goal("check")
            .requires("checkstyleMainReport", "checkstyleTestReport")
            .register();
    }

}
