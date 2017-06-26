package jobt.plugin.checkstyle;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class CheckstylePlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register("checkstyleMain",
            new CheckstyleTask(CompileTarget.MAIN), provides());

        taskRegistry.register("checkstyleTest",
            new CheckstyleTask(CompileTarget.TEST), provides());
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("checkstyleMain").uses(
            taskTemplate.product("compileDependencies")
            );

        taskTemplate.task("checkstyleTest").uses(
            taskTemplate.product("testDependencies")
            );

        taskTemplate.task("check").uses(
            taskTemplate.product("compileDependencies"),
            taskTemplate.product("testDependencies"));
    }

}
