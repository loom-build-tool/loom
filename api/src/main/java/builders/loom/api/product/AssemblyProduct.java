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
import java.util.Optional;

/**
 * E.g. a "jar" file.
 *
 */
public final class AssemblyProduct extends AbstractProduct {

    private final Path assemblyFile;
    private final String outputInfo;
    private final String hash;

    public AssemblyProduct(final Path assemblyFile, final String outputInfo) {
        this(assemblyFile, outputInfo, null);
    }

    public AssemblyProduct(final Path assemblyFile, final String outputInfo,
                           final String hash) {
        this.assemblyFile = assemblyFile;
        this.outputInfo = outputInfo;
        this.hash = hash;
    }

    public Path getAssemblyFile() {
        return assemblyFile;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public Optional<String> outputInfo() {
        if (outputInfo == null) {
            return Optional.empty();
        }

        return Optional.of(outputInfo + ": " + assemblyFile);
    }

    @Override
    public String toString() {
        return "AssemblyProduct{"
            + "assemblyFile=" + assemblyFile
            + ", outputInfo='" + outputInfo + '\''
            + ", hash='" + hash + '\''
            + '}';
    }

}
