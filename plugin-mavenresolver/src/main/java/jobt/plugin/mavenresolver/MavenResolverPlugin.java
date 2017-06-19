package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        taskRegistry.register("resolveCompileDependencies",
            new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig(),
                uses(), provides("compileDependencies")));

        taskRegistry.register("resolveTestDependencies",
            new MavenResolverTask(DependencyScope.TEST, getBuildConfig(),
                uses(), provides("testDependencies")));

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

    }

}
