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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SourceTreeProduct extends AbstractProduct {

    private final Path srcDir;
    private final Map<String, List<Path>> sourceFiles;

    public SourceTreeProduct(final Path srcDir, final Map<String, List<Path>> sourceFiles) {
        this.srcDir = Objects.requireNonNull(srcDir, "srcDir must not be null");
        if (sourceFiles == null || sourceFiles.isEmpty()) {
            throw new IllegalArgumentException("sourceFiles must not be null or empty");
        }
        this.sourceFiles = Collections.unmodifiableMap(new HashMap<>(sourceFiles));
    }

    public Path getSrcDir() {
        return srcDir;
    }

    public Map<String, List<Path>> getSourceFiles() {
        return sourceFiles;
    }

}
