package jobt.api;

import java.nio.file.Path;
import java.util.List;

public interface DependencyResolver {

    List<Path> buildClasspath(List<String> deps, String scope);

}
