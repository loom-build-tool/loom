package jobt.plugin.checkstyle;

import java.util.Collections;
import java.util.List;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.ExecutionContext;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class CheckstylePlugin extends AbstractPlugin {

    @Override
    public List<String> dependencies() {
        return Collections.singletonList("com.puppycrawl.tools:checkstyle:7.8");
    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("check").dependsOn(
            taskTemplate.task("checkstyleMain"),
            taskTemplate.task("checkstyleTest"));
    }

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final ExecutionContext executionContext = getExecutionContext();

        taskRegistry.register("checkstyleMain",
            new CheckstyleTask(CompileTarget.MAIN, executionContext));

        taskRegistry.register("checkstyleTest",
            new CheckstyleTask(CompileTarget.TEST, executionContext));
    }

}
