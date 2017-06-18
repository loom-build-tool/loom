package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {

        final MavenResolver mavenResolver = new MavenResolver();

        taskRegistry.register("resolveCompileDependencies",
            new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig(),
                mavenResolver,
                uses(), provides("compileDependencies")));

        taskRegistry.register("resolveTestDependencies",
            new MavenResolverTask(DependencyScope.TEST, getBuildConfig(),
                mavenResolver,
                uses(), provides("testDependencies")));

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {

    }

}
