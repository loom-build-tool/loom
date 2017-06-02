package jobt.plugin;

import java.util.List;
import java.util.Map;

public interface BuildConfig {

    Project getProject();

    List<String> getPlugins();

    Map<String, String> getConfiguration();

    List<String> getDependencies();

    List<String> getTestDependencies();

}
