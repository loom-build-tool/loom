package builders.loom.api;

import java.util.Map;
import java.util.Set;

public class GlobalBuildConfig implements BuildConfig, BuildConfigWithSettings {

    @Override
    public Set<String> getPlugins() {
        return Set.of();
    }

    @Override
    public Map<String, String> getSettings() {
        return Map.of();
    }

}
