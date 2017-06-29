package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure() {
        final ProgressIndicator progressIndicator =
            new ProgressIndicator("mavenResolver");

        final MavenResolver resolver = new MavenResolver();
        resolver.setProgressIndicator(progressIndicator);

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig(), resolver))
            .provides("compileDependencies")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, getBuildConfig(), resolver))
            .provides("testDependencies")
            .register();
    }

}
