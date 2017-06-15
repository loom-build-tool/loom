package jobt.plugin.mavenresolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import jobt.api.DependencyScope;

public interface DependencyResolver {

    List<Path> resolve(List<String> deps, DependencyScope scope)
        throws IOException;

}
