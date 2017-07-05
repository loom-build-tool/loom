package jobt.plugin.mavenresolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jobt.api.AbstractTask;
import jobt.api.BuildConfig;
import jobt.api.DependencyScope;
import jobt.api.TaskStatus;
import jobt.api.product.ArtifactProduct;
import jobt.api.product.ClasspathProduct;

public class MavenResolverTask extends AbstractTask {

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private MavenResolver mavenResolver;

    public MavenResolverTask(final DependencyScope dependencyScope,
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
        final List<ArtifactProduct> artifactProducts = mavenResolver.resolve(dependencies,
            DependencyScope.COMPILE, null);

        final List<Path> collect = artifactProducts.stream()
            .map(ArtifactProduct::getMainArtifact).collect(Collectors.toList());

        getProvidedProducts().complete("compileDependencies",
            new ClasspathProduct(collect));
    }

    private void testScope() {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        final List<ArtifactProduct> artifactProducts = mavenResolver.resolve(dependencies,
            DependencyScope.TEST, null);

        final List<Path> collect = artifactProducts.stream()
            .map(ArtifactProduct::getMainArtifact).collect(Collectors.toList());

        getProvidedProducts().complete("testDependencies",
            new ClasspathProduct(collect));
    }

}
