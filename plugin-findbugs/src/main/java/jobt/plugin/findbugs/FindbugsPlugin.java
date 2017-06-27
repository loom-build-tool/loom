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
            new FindbugsTask(getBuildConfig(), CompileTarget.MAIN), provides("findbugsMainReport"));

        taskRegistry.register(
            "findbugsTest",
            new FindbugsTask(getBuildConfig(), CompileTarget.TEST), provides("findbugsTestReport"));

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("findbugsMain")
            .uses(
                taskTemplate.product("source"),
                taskTemplate.product("compileDependencies"),
                taskTemplate.product("compilation")
                );

        taskTemplate.task("findbugsTest")
            .uses(
                taskTemplate.product("testSource"),
                taskTemplate.product("testDependencies"),
                taskTemplate.product("compilation"),
                taskTemplate.product("testCompilation")
            );

        taskTemplate.virtualProduct("check").uses(
            taskTemplate.product("findbugsMainReport"),
            taskTemplate.product("findbugsTestReport"));

    }

}
