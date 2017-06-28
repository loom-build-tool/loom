package jobt.plugin.checkstyle;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;

public class CheckstylePlugin extends AbstractPlugin {

    @Override
    public void configure() {
        task("checkstyleMain")
            .impl(() -> new CheckstyleTask(CompileTarget.MAIN))
            .uses("source", "compileDependencies")
            .provides("checkstyleMainReport")
            .register();

        task("checkstyleTest")
            .impl(() -> new CheckstyleTask(CompileTarget.TEST))
            .uses("testSource", "testDependencies")
            .provides("checkstyleTestReport")
            .register();

        goal("check")
            .requires("checkstyleMainReport", "checkstyleTestReport")
            .register();
    }

}
