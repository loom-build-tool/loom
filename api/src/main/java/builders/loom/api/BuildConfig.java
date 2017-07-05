package builders.loom.api;

import java.util.Set;

public interface BuildConfig {

    Project getProject();

    Set<String> getPlugins();

    BuildSettings getBuildSettings();

    Set<String> getDependencies();

    Set<String> getTestDependencies();

}
