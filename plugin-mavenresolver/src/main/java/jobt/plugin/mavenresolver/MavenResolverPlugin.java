package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;

public class MavenResolverPlugin extends AbstractPlugin {

    @Override
    public void configure() {

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, getBuildConfig()))
            .provides("compileDependencies")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, getBuildConfig()))
            .provides("testDependencies")
            .register();
    }

}
