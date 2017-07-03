package jobt.plugin.mavenresolver;

import java.nio.file.Path;
import java.util.List;

import jobt.api.AbstractPlugin;
import jobt.api.DependencyResolverService;
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


        service("mavenDependencyResolver")
            .impl(() -> new DependencyResolverService() {
                @Override
                public List<Path> resolve(final List<String> deps,
                    final DependencyScope scope, final String cacheName) {
                    final MavenResolver mavenResolver = MavenResolverSingleton.getInstance();
                    return mavenResolver.resolve(deps, scope, cacheName);
                }
            })
            .register();
    }

}
