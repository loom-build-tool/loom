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

package builders.loom.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import builders.loom.api.JavaVersion;

public class BuildConfigDTO {

    private static final JavaVersion DEFAULT_JAVA_PLATFORM_VERSION = JavaVersion.JAVA_1_8;

    private ProjectDTO project;
    private Set<String> plugins;
    private Map<String, String> settings;
    private Set<String> moduleDependencies;
    private Set<String> dependencies;
    private Set<String> testDependencies;

    public ProjectDTO getProject() {
        return project;
    }

    public void setProject(final ProjectDTO project) {
        this.project = project;
    }

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

    public Set<String> getModuleDependencies() {
        return moduleDependencies;
    }

    public void setModuleDependencies(final Set<String> moduleDependencies) {
        this.moduleDependencies = moduleDependencies;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(final Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public void setTestDependencies(final Set<String> testDependencies) {
        this.testDependencies = testDependencies;
    }

    public BuildConfigImpl build() {
        final Map<String, String> cfg = settings != null ? settings : new HashMap<>();

        final String javaPlatformVersionStr = cfg.remove("javaPlatformVersion");
        final JavaVersion javaPlatformVersion = javaPlatformVersionStr != null
            ? JavaVersion.ofVersion(javaPlatformVersionStr)
            : DEFAULT_JAVA_PLATFORM_VERSION;

        final BuildSettingsImpl buildSettings = new BuildSettingsImpl(javaPlatformVersion);

        return new BuildConfigImpl(
            project.build(),
            plugins != null ? plugins : Collections.emptySet(),
            buildSettings,
            cfg,
            moduleDependencies, dependencies != null ? dependencies : Collections.emptySet(),
            testDependencies != null ? testDependencies : Collections.emptySet());
    }

}
