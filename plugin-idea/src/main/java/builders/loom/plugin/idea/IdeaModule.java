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

package builders.loom.plugin.idea;

import java.nio.file.Path;
import java.util.Objects;

public class IdeaModule {

    private final Path imlFile;
    private final String moduleName;

    public IdeaModule(final Path imlFile, final String moduleName) {
        this.imlFile = Objects.requireNonNull(imlFile, "imlFile required");
        this.moduleName = Objects.requireNonNull(moduleName, "moduleName required");
    }

    public Path getImlFile() {
        return imlFile;
    }

    public String getModuleName() {
        return moduleName;
    }

}
