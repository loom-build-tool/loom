package builders.loom.plugin.mavenresolver;

import java.util.List;

import builders.loom.api.DependencyScope;
import builders.loom.api.product.ArtifactProduct;

public interface DependencyResolver {

    List<ArtifactProduct> resolve(List<String> deps, DependencyScope scope, String classifier);

}
