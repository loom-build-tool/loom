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
import java.util.List;
import java.util.Objects;

public final class ResourcesTreeProduct extends AbstractProduct {

    private final Path srcDir;
    private final List<Path> srcFiles;

    public ResourcesTreeProduct(final Path srcDir, final List<Path> srcFiles) {
        this.srcDir = Objects.requireNonNull(srcDir, "srcDir must not be null");
        if (srcFiles == null || srcFiles.isEmpty()) {
            throw new IllegalArgumentException("srcFiles must not be null or empty");
        }
        this.srcFiles = Collections.unmodifiableList(srcFiles);
    }

    public Path getSrcDir() {
        return srcDir;
    }

    public List<Path> getSrcFiles() {
        return srcFiles;
    }

    @Override
    public String toString() {
        return "ResourcesTreeProduct{"
            + "srcDir=" + srcDir
            + ", srcFiles=" + srcFiles
            + '}';
    }

}
