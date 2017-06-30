package jobt.config;

import jobt.api.BuildSettings;
import jobt.api.JavaVersion;

class BuildSettingsImpl implements BuildSettings {

    private final JavaVersion javaPlatformVersion;

    BuildSettingsImpl(final JavaVersion javaPlatformVersion) {
        this.javaPlatformVersion = javaPlatformVersion;
    }

    @Override
    public JavaVersion getJavaPlatformVersion() {
        return javaPlatformVersion;
    }

    @Override
    public String toString() {
        return "BuildSettingsImpl{"
            + "javaPlatformVersion=" + javaPlatformVersion
            + '}';
    }

}
