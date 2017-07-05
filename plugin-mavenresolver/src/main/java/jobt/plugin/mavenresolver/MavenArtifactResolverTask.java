package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.TaskStatus;
import jobt.api.product.ArtifactListProduct;

public class MavenArtifactResolverTask extends AbstractTask {

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private MavenResolver mavenResolver;

    public MavenArtifactResolverTask(final DependencyScope dependencyScope,
                                     final BuildConfig buildConfig) {

        this.dependencyScope = dependencyScope;
        this.buildConfig = buildConfig;
    }

    @Override
    public TaskStatus run() throws Exception {
        this.mavenResolver = MavenResolverSingleton.getInstance();
        switch (dependencyScope) {
            case COMPILE:
                compileScope();
                return TaskStatus.OK;
            case TEST:
                testScope();
                return TaskStatus.OK;
            default:
                throw new IllegalStateException("Unknown scope: " + dependencyScope);
        }
    }

    private void compileScope() {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        getProvidedProducts().complete("compileArtifacts",
            new ArtifactListProduct(mavenResolver.resolve(dependencies,
                DependencyScope.COMPILE, "sources", "main")));
    }

    private void testScope() {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        getProvidedProducts().complete("testArtifacts",
            new ArtifactListProduct(mavenResolver.resolve(dependencies,
                DependencyScope.TEST, "sources", "test")));
    }
}
