package jobt.plugin.mavenresolver;

import java.nio.file.Path;
import java.util.List;

import jobt.api.DependencyScope;

public interface DependencyResolver {

    List<Path> resolve(List<String> deps, DependencyScope scope);

}
