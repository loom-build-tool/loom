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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class DirectoryProduct extends AbstractProduct {

    private final Path dir;
    private final String outputInfo;

    public DirectoryProduct(final Path dir, final String outputInfo) {
        this.dir = Objects.requireNonNull(dir);
        this.outputInfo = outputInfo;

        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Path " + dir + " is not an existing dir");
        }
    }

    public Path getDir() {
        return dir;
    }

    @Override
    public Optional<String> outputInfo() {
        if (outputInfo == null) {
            return Optional.empty();
        }

        return Optional.of(outputInfo + ": " + dir);
    }

}
