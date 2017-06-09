package jobt.plugin.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;


public class FindBugsPlugin extends AbstractPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FindBugsPlugin.class);

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register(
            "findbugsMain",
            new FindbugsTask(CompileTarget.MAIN, getExecutionContext()));

        taskRegistry.register(
            "findBugsTest",
            new FindbugsTask(CompileTarget.TEST, getExecutionContext()));

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("findbugsMain")
            .dependsOn(taskTemplate.task("classes"));

        taskTemplate.task("findBugsTest")
        .dependsOn(
            taskTemplate.task("classes"),
            taskTemplate.task("testClasses")
            );

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("findbugsMain"),
            taskTemplate.task("findBugsTest"));

    }



}
