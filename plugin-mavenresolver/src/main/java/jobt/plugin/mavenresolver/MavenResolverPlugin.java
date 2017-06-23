package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.Classpath;
import jobt.api.DependencyScope;
import jobt.api.TaskRegistry;
import jobt.api.TaskTemplate;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure(final TaskRegistry taskRegistry) {
        final ProgressIndicator progressIndicator = new ProgressIndicator("mavenResolver");

        final MavenResolver mavenResolver = new MavenResolver();
        mavenResolver.setProgressIndicator(progressIndicator);

        taskRegistry.register("resolveCompileDependencies",
            new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig(),
                mavenResolver,
                uses(), provides(Classpath.class, "compileDependencies")));

        taskRegistry.register("resolveTestDependencies",
            new MavenResolverTask(DependencyScope.TEST, getBuildConfig(),
                mavenResolver,
                uses(), provides(Classpath.class, "testDependencies")));

    }

    @Override
    public void configure(final TaskTemplate taskTemplate) {
        taskTemplate.task("resolveTestDependencies")
            .dependsOn(taskTemplate.task("resolveCompileDependencies"));
    }

}
