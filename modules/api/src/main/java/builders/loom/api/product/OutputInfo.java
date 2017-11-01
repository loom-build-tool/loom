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

package builders.loom.api.product;

import java.nio.file.Path;
import java.util.Objects;

public final class OutputInfo {

    private final String name;
    private final Path artifact;

    public OutputInfo(final String name) {
        this(name, null);
    }

    public OutputInfo(final String name, final Path artifact) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.artifact = artifact;
    }

    public String getName() {
        return name;
    }

    public Path getArtifact() {
        return artifact;
    }

    @Override
    public String toString() {
        return "OutputInfo{"
            + "name='" + name + '\''
            + ", artifact='" + artifact + '\''
            + '}';
    }

}
