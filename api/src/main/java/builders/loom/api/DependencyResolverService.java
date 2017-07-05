package builders.loom.api;

import java.nio.file.Path;
import java.util.List;

public interface DependencyResolverService extends Service {

    List<Path> resolve(final List<String> deps, final DependencyScope scope,
                       final String cacheName);

}
