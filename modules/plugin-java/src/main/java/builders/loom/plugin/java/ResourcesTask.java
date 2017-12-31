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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.RepositoryPathAware;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ManagedGenericProduct;
import builders.loom.api.product.Product;
import builders.loom.util.Hashing;
import builders.loom.util.ProductChecksumUtil;

public class ResourcesTask extends AbstractModuleTask implements RepositoryPathAware {

    private static final NullKeyValueCache NULL_CACHE = new NullKeyValueCache();

    private final JavaPluginSettings pluginSettings;
    private final CompileTarget compileTarget;
    private final String resourcesProductId;
    private Path repositoryPath;

    ResourcesTask(final JavaPluginSettings pluginSettings,
                  final CompileTarget compileTarget) {

        this.pluginSettings = pluginSettings;
        this.compileTarget = compileTarget;

        switch (compileTarget) {
            case MAIN:
                resourcesProductId = "resources";
                break;
            case TEST:
                resourcesProductId = "testResources";
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public void setRepositoryPath(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<Product> resourcesProduct =
            useProduct(resourcesProductId, Product.class);

        if (!resourcesProduct.isPresent()) {
            return TaskResult.empty();
        }

        final Path buildDir =
            Files.createDirectories(resolveBuildDir("resources", compileTarget));

        final Path resDir = Paths.get(resourcesProduct.get().getProperty("resDir"));

        final KeyValueCache cache = initCache();

        Files.walkFileTree(resDir, newCopyVisitor(buildDir, cache));
        Files.walkFileTree(buildDir, new DeleteObsoleteFileVisitor(resDir, cache));

        cache.saveCache();

        return TaskResult.done(newProduct(buildDir));
    }

    private CopyFileVisitor newCopyVisitor(final Path buildDir, final KeyValueCache cache)
        throws IOException {

        final String glob = pluginSettings.getResourceFilterGlob();
        if (glob != null) {
            return new CopyFileVisitor(buildDir, cache, glob, buildVariablesMap());
        }

        return new CopyFileVisitor(buildDir, cache);
    }

    private KeyValueCache initCache() {
        if (!getRuntimeConfiguration().isCacheEnabled()) {
            return NULL_CACHE;
        }

        // hash has to change for filter glob changes -- filtered files must not be cached
        final String hash =
            Hashing.hash(Objects.toString(pluginSettings.getResourceFilterGlob()));

        final Path cacheFile = repositoryPath
            .resolve(getBuildContext().getModuleName())
            .resolve(compileTarget.name().toLowerCase())
            .resolve("resource-" + hash + ".cache");

        return new DiskKeyValueCache(cacheFile);
    }

    private Function<String, Optional<String>> buildVariablesMap() {
        return (placeholder) -> {
            if ("project.version".equals(placeholder)) {
                return Optional.ofNullable(getRuntimeConfiguration().getVersion());
            }

            return Optional.ofNullable(System.getProperty(placeholder))
                .or(() -> Optional.ofNullable(System.getenv(placeholder)));
        };
    }

    private static Product newProduct(final Path buildDir) {
        return new ManagedGenericProduct("processedResourcesDir", buildDir.toString(),
            ProductChecksumUtil.recursiveContentChecksum(buildDir), null);
    }
}
