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

import java.util.Objects;

import builders.loom.api.BuildSettings;
import builders.loom.api.JavaVersion;

public class BuildSettingsImpl implements BuildSettings {

    private static final JavaVersion DEFAULT_JAVA_PLATFORM_VERSION = JavaVersion.JAVA_9;

    private final JavaVersion javaPlatformVersion;

    public BuildSettingsImpl() {
        this(DEFAULT_JAVA_PLATFORM_VERSION);
    }

    public BuildSettingsImpl(final JavaVersion javaPlatformVersion) {
        this.javaPlatformVersion = Objects.requireNonNull(javaPlatformVersion,
            "javaPlatformVersion required");
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
