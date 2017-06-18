package jobt.plugin.checkstyle;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.ExecutionContext;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class CheckstylePlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final ExecutionContext executionContext = getExecutionContext();

        taskRegistry.register("checkstyleMain",
            new CheckstyleTask(CompileTarget.MAIN,
                uses("compileClasspath"), provides()));

        taskRegistry.register("checkstyleTest",
            new CheckstyleTask(CompileTarget.TEST,
                uses("testClasspath"), provides()));
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("check").dependsOn(
            taskTemplate.task("checkstyleMain"),
            taskTemplate.task("checkstyleTest"));
    }

}
