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

package builders.loom.plugin.java;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarOutputStream;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.ResourcesTreeProduct;
import builders.loom.api.product.SourceTreeProduct;
import builders.loom.util.Hasher;

public class JavaAssembleSourcesJarTask extends AbstractModuleTask {

    private final Path repositoryPath;

    public JavaAssembleSourcesJarTask(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<ResourcesTreeProduct> resourcesTreeProduct = useProduct(
            "resources", ResourcesTreeProduct.class);

        final Optional<SourceTreeProduct> sourceTree = useProduct(
            "source", SourceTreeProduct.class);

        if (!resourcesTreeProduct.isPresent() && !sourceTree.isPresent()) {
            return completeEmpty();
        }

        final String productHash = computeHash(resourcesTreeProduct, sourceTree);

        final Path buildDir = LoomPaths.buildDir(
            getRuntimeConfiguration().getProjectBaseDir(),
                getBuildContext().getModuleName(), "sources-jar");

        final Path sourceJarFile = buildDir.resolve(String.format("%s-sources.jar",
            getBuildContext().getModuleName()));

        if (Files.exists(sourceJarFile) && checkHash(productHash)) {
            log.error("UP 2 DATE");
            return completeUpToDate(new AssemblyProduct(sourceJarFile, "Jar of sources",
                productHash));
        }

        Files.createDirectories(buildDir);

        try (final JarOutputStream os = new JarOutputStream(Files.newOutputStream(sourceJarFile))) {
            if (resourcesTreeProduct.isPresent()) {
                FileUtil.copy(resourcesTreeProduct.get().getSrcDir(), os);
            }

            if (sourceTree.isPresent()) {
                FileUtil.copy(sourceTree.get().getSrcDir(), os);
            }
        }

        writeHash(productHash);

        return completeOk(new AssemblyProduct(sourceJarFile, "Jar of sources",
            productHash));
    }

    private String computeHash(final Optional<ResourcesTreeProduct> resourcesTreeProduct,
                               final Optional<SourceTreeProduct> sourceTree) {
        return new Hasher()
            .putString(resourcesTreeProduct.map(ResourcesTreeProduct::getHash)
                .orElse("no resources"))
            .putString(sourceTree.map(SourceTreeProduct::getHash)
                .orElse("no sources"))
            .stringHash();
    }

    private boolean checkHash(final String hash) {
        try {
            final Path hashFile = repositoryPath.resolve(resolveHashFile());
            if (Files.notExists(hashFile)) {
                return false;
            }
            final List<String> strings = Files.readAllLines(hashFile);

            Files.delete(hashFile);

            final String hashFromFile = strings.get(0);

            return hash.equals(hashFromFile);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveHashFile() {
        return Paths.get("foo-" + getBuildContext().getModuleName() + ".hash");
    }

    private void writeHash(final String hash) {
        try {
            Files.write(Files.createDirectories(repositoryPath).resolve(resolveHashFile()), List.of(hash), StandardOpenOption.CREATE_NEW);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
