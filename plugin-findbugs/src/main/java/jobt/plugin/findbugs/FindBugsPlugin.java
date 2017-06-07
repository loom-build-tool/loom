package jobt.plugin.findbugs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.AbstractPlugin;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;


public class FindBugsPlugin extends AbstractPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FindBugsPlugin.class);

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register("findbugsMain", new FindbugsTask(getExtClassLoader(), getExecutionContext(), getDependencyResolver()));

        taskRegistry.register("findbugsGoGo", new FindbugsTask(getExtClassLoader(), getExecutionContext(), getDependencyResolver()));

//        taskRegistry.register("findBugsTest", task); // TODO

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

        taskTemplate.task("findbugsGoGo");

        taskTemplate.task("findbugsMain")
            .dependsOn(taskTemplate.task("classes"));

        taskTemplate.task("check").dependsOn(
            taskTemplate.task("findbugsMain"));

    }



}
