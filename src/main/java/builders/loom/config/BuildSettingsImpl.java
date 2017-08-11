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

import java.lang.module.ModuleDescriptor;
import java.util.Objects;

import builders.loom.api.BuildSettings;
import builders.loom.api.JavaVersion;

public class BuildSettingsImpl implements BuildSettings {

    private final String moduleName;
    private final JavaVersion javaPlatformVersion;

    public BuildSettingsImpl(final String moduleName, final JavaVersion javaPlatformVersion) {
        if (moduleName != null) {
            // Validate module name
            ModuleDescriptor.newModule(moduleName);
        }
        this.moduleName = moduleName;
        this.javaPlatformVersion = Objects.requireNonNull(javaPlatformVersion,
            "javaPlatformVersion required");
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public JavaVersion getJavaPlatformVersion() {
        return javaPlatformVersion;
    }

    @Override
    public String toString() {
        return "BuildSettingsImpl{"
            + "moduleName='" + moduleName + '\''
            + ", javaPlatformVersion=" + javaPlatformVersion
            + '}';
    }

}
