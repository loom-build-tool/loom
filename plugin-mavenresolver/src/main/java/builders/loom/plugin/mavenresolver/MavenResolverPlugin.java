package builders.loom.plugin.mavenresolver;

import java.util.stream.Collectors;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.BuildConfig;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.PluginSettings;

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
