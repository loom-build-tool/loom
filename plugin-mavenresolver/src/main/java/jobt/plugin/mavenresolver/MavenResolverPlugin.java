package jobt.plugin.mavenresolver;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyScope;
import jobt.api.PluginSettings;

public class MavenResolverPlugin extends AbstractPlugin<PluginSettings> {

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
