/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.core.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import builders.loom.api.BuildSettings;
import builders.loom.api.JavaVersion;

public class BuildConfigDTO {

    private Set<String> plugins;
    private Map<String, String> settings;
    private Set<String> moduleCompileDependencies;
    private Set<String> compileDependencies;
    private Set<String> testDependencies;
    private Set<String> globalExcludes;

    public Set<String> getPlugins() {
        return plugins;
    }

    public void setPlugins(final Set<String> plugins) {
        this.plugins = plugins;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(final Map<String, String> settings) {
        this.settings = settings;
    }

    public Set<String> getModuleCompileDependencies() {
        return moduleCompileDependencies;
    }

    public void setModuleCompileDependencies(final Set<String> moduleCompileDependencies) {
        this.moduleCompileDependencies = moduleCompileDependencies;
    }

    public Set<String> getCompileDependencies() {
        return compileDependencies;
    }

    public void setCompileDependencies(final Set<String> compileDependencies) {
        this.compileDependencies = compileDependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final Set<String> testDependencies) {
        this.testDependencies = testDependencies;
    }

    public Set<String> getGlobalExcludes() {
        return globalExcludes;
    }

    public void setGlobalExcludes(final Set<String> globalExcludes) {
        this.globalExcludes = globalExcludes;
    }

    public BuildConfigImpl build() {
        final Map<String, String> cfg = settings != null ? settings : new HashMap<>();

        final String moduleName = cfg.remove("moduleName");

        final JavaVersion javaPlatformVersion =
            Optional.ofNullable(cfg.remove("javaPlatformVersion"))
                .map(JavaVersion::ofVersion)
                .orElse(BuildConfigImpl.DEFAULT_JAVA_PLATFORM_VERSION);

        final BuildSettings buildSettings = new BuildSettingsImpl(moduleName, javaPlatformVersion);

        return new BuildConfigImpl(
            nullToEmpty(plugins),
            buildSettings,
            cfg,
            nullToEmpty(moduleCompileDependencies),
            nullToEmpty(compileDependencies),
            nullToEmpty(testDependencies),
            nullToEmpty(globalExcludes));
    }

    private static Set<String> nullToEmpty(final Set<String> set) {
        return set != null ? set : Collections.emptySet();
    }

}
