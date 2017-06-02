package jobt.api;

import java.io.File;
import java.util.List;

public interface DependencyResolver {

    List<File> buildClasspath(List<String> deps, String scope);

}
