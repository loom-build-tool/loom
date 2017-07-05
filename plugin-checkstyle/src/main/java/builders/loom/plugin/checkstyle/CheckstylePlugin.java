package builders.loom.plugin.checkstyle;

import builders.loom.api.CompileTarget;
import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;

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
