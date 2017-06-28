package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        final ProgressIndicator progressIndicator = new ProgressIndicator("mavenResolver");

        final MavenResolver mavenResolver = new MavenResolver();
        mavenResolver.setProgressIndicator(progressIndicator);

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig(), mavenResolver))
            .provides("compileDependencies")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, getBuildConfig(), mavenResolver))
            .provides("testDependencies")
            .register();
    }

}
