package jobt.api;

import java.util.Map;
import java.util.Set;

public interface BuildConfig {

    Project getProject();

    Set<String> getPlugins();

    Map<String, String> getConfiguration();

    Set<String> getDependencies();

    Set<String> getTestDependencies();

}
