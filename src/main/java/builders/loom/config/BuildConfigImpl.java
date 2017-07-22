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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import builders.loom.api.BuildConfigWithSettings;
import builders.loom.api.BuildSettings;
import builders.loom.api.Project;

class BuildConfigImpl implements BuildConfigWithSettings, Serializable {

    private static final long serialVersionUID = 1L;

    private final Project project;
    private final Set<String> plugins;
    private final BuildSettings buildSettings;
    private final Map<String, String> settings;
    private final Set<String> moduleDependencies;
    private final Set<String> dependencies;
    private final Set<String> testDependencies;

    BuildConfigImpl(final ProjectImpl project, final Set<String> plugins,
                    final BuildSettings buildSettings,
                    final Map<String, String> settings,
                    final Set<String> moduleDependencies, final Set<String> dependencies,
                    final Set<String> testDependencies) {
        this.project = Objects.requireNonNull(project);
        this.plugins = Collections.unmodifiableSet(Objects.requireNonNull(plugins));
        this.buildSettings = Objects.requireNonNull(buildSettings);
        this.settings = Collections.unmodifiableMap(Objects.requireNonNull(settings));
        this.moduleDependencies = Collections.unmodifiableSet(Objects.requireNonNull(moduleDependencies));
        this.dependencies = Collections.unmodifiableSet(Objects.requireNonNull(dependencies));
        this.testDependencies =
            Collections.unmodifiableSet(Objects.requireNonNull(testDependencies));
    }

    @Override
    public Project getProject() {
        return project;
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
    public Set<String> getModuleDependencies() {
        return moduleDependencies;
    }

    @Override
    public Set<String> getDependencies() {
        return dependencies;
    }

    @Override
    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    @Override
    public String toString() {
        return "BuildConfigImpl{"
            + "project=" + project
            + ", plugins=" + plugins
            + ", buildSettings=" + buildSettings
            + ", settings=" + settings
            + ", moduleDependencies=" + moduleDependencies
            + ", dependencies=" + dependencies
            + ", testDependencies=" + testDependencies
            + '}';
    }

}
