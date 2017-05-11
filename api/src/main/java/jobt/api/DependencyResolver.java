package jobt.api;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Future;

public interface DependencyResolver {

    Future<List<Path>> resolveDependencies(List<String> dependencies, String scope);

}
