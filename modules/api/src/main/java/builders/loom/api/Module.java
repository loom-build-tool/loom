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

package builders.loom.api;

import java.nio.file.Path;
import java.util.Objects;

public class Module implements BuildContext {

    public static final String UNNAMED_MODULE = "unnamed";

    private final String moduleName;
    private final Path path;
    private final ModuleBuildConfig config;

    public Module(final String moduleName, final Path path, final ModuleBuildConfig config) {
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName required");
        this.path = Objects.requireNonNull(path, "path required");
        this.config = Objects.requireNonNull(config, "config required");
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public ModuleBuildConfig getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "Module{"
            + "moduleName='" + moduleName + '\''
            + ", path=" + path
            + ", config=" + config
            + '}';
    }

}
