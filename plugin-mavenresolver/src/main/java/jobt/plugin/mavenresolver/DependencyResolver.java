package jobt.plugin.mavenresolver;

import java.util.List;

import jobt.api.DependencyScope;
import jobt.api.product.ArtifactProduct;

public interface DependencyResolver {

    List<ArtifactProduct> resolve(List<String> deps, DependencyScope scope, String classifier,
                                  String cacheName);

}
