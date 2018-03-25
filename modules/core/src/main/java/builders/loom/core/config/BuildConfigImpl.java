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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import builders.loom.api.BuildSettings;
import builders.loom.api.JavaVersion;
import builders.loom.api.ModuleBuildConfig;

public class BuildConfigImpl implements ModuleBuildConfig, Serializable {

    static final JavaVersion DEFAULT_JAVA_PLATFORM_VERSION = JavaVersion.JAVA_10;

    private static final long serialVersionUID = 1L;

    private final Set<String> plugins;
    private final BuildSettings buildSettings;
    private final Map<String, String> settings;
    private final Set<String> moduleCompileDependencies;
    private final Set<String> compileDependencies;
    private final Set<String> testDependencies;

    public BuildConfigImpl() {
        this(Set.of(), new BuildSettingsImpl(null, DEFAULT_JAVA_PLATFORM_VERSION),
            Map.of(), Set.of(), Set.of(), Set.of());
    }

    public BuildConfigImpl(final Set<String> plugins,
                           final BuildSettings buildSettings,
                           final Map<String, String> settings,
                           final Set<String> moduleCompileDependencies,
                           final Set<String> compileDependencies,
                           final Set<String> testDependencies) {
        this.plugins = Collections.unmodifiableSet(Objects.requireNonNull(plugins));
        this.buildSettings = Objects.requireNonNull(buildSettings);
        this.settings = Collections.unmodifiableMap(Objects.requireNonNull(settings));
        this.moduleCompileDependencies =
            Collections.unmodifiableSet(Objects.requireNonNull(moduleCompileDependencies));
        this.compileDependencies =
            Collections.unmodifiableSet(Objects.requireNonNull(compileDependencies));
        this.testDependencies =
            Collections.unmodifiableSet(Objects.requireNonNull(testDependencies));
    }

    @Override
    public Set<String> getPlugins() {
        return plugins;
    }

    @Override
    public BuildSettings getBuildSettings() {
        return buildSettings;
    }

    @Override
    public Map<String, String> getSettings() {
        return settings;
    }

    @Override
    public Set<String> getModuleCompileDependencies() {
        return moduleCompileDependencies;
    }

    @Override
    public Set<String> getCompileDependencies() {
        return compileDependencies;
    }

    @Override
    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    @Override
    public String toString() {
        return "BuildConfigImpl{"
            + "plugins=" + plugins
            + ", buildSettings=" + buildSettings
            + ", settings=" + settings
            + ", moduleCompileDependencies=" + moduleCompileDependencies
            + ", compileDependencies=" + compileDependencies
            + ", testDependencies=" + testDependencies
            + '}';
    }

}
