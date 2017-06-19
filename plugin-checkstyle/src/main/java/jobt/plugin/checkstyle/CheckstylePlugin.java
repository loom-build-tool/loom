package jobt.plugin.checkstyle;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class CheckstylePlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register("checkstyleMain",
            new CheckstyleTask(CompileTarget.MAIN,
                uses("compileDependencies"), provides()));

        taskRegistry.register("checkstyleTest",
            new CheckstyleTask(CompileTarget.TEST,
                uses("testDependencies"), provides()));
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("checkstyleMain").dependsOn(
            taskTemplate.task("resolveCompileDependencies"),
            taskTemplate.task("compileJava")
            );

        taskTemplate.task("checkstyleTest").dependsOn(
            taskTemplate.task("resolveTestDependencies"),
            taskTemplate.task("compileTestJava")
            );

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("checkstyleMain"),
            taskTemplate.task("checkstyleTest"));
    }

}
