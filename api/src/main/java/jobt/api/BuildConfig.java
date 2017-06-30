package jobt.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BuildConfig {

    Project getProject();

    Set<String> getPlugins();

    Map<String, String> getConfiguration();

    Optional<String> lookupConfiguration(String key);

    Set<String> getDependencies();

    Set<String> getTestDependencies();

}
