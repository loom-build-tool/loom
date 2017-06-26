package jobt.plugin.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.AbstractPlugin;
import jobt.api.CompileTarget;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class FindbugsPlugin extends AbstractPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FindbugsPlugin.class);

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register(
            "findbugsMain",
            new FindbugsTask(getBuildConfig(), CompileTarget.MAIN,
                uses("compileDependencies")), provides());

        taskRegistry.register(
            "findbugsTest",
            new FindbugsTask(getBuildConfig(), CompileTarget.TEST,
                uses("testDependencies")), provides());

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("findbugsMain")
            .dependsOn(taskTemplate.task("classes"));

        taskTemplate.task("findbugsTest")
            .dependsOn(
                taskTemplate.task("classes"),
                taskTemplate.task("testClasses"));

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("findbugsMain"),
            taskTemplate.task("findbugsTest"));

    }

}
