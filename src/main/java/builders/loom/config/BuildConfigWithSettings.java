package builders.loom.config;

import java.util.Map;

import builders.loom.api.BuildConfig;

public interface BuildConfigWithSettings extends BuildConfig {

    Map<String, String> getSettings();

}
