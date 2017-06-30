package jobt.config;

import java.util.Map;

import jobt.api.BuildConfig;

public interface BuildConfigWithSettings extends BuildConfig {

    Map<String, String> getSettings();

}
