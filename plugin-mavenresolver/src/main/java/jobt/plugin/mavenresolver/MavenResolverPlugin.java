package jobt.plugin.mavenresolver;

import java.util.stream.Collectors;

import jobt.api.AbstractPlugin;
import jobt.api.BuildConfig;
import jobt.api.DependencyResolverService;
import jobt.api.DependencyScope;
import jobt.api.PluginSettings;
import jobt.api.product.ArtifactProduct;

public class MavenResolverPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        final BuildConfig cfg = getBuildConfig();

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, cfg))
            .provides("compileDependencies")
            .register();

        task("resolveCompileArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.COMPILE, cfg))
            .provides("compileArtifacts")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, cfg))
            .provides("testDependencies")
            .register();

        task("resolveTestArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.TEST, cfg))
            .provides("testArtifacts")
            .register();

        service("mavenDependencyResolver")
            .impl(() -> (DependencyResolverService) (deps, scope, cacheName) ->
                MavenResolverSingleton.getInstance()
                    .resolve(deps, scope, null).stream()
                    .map(ArtifactProduct::getMainArtifact)
                    .collect(Collectors.toList()))
            .register();
    }

}
