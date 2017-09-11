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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ResourcesTreeProduct;
import builders.loom.util.Hasher;

public class ResourcesTask extends AbstractModuleTask {

    private static final NullKeyValueCache NULL_CACHE = new NullKeyValueCache();

    private final JavaPluginSettings pluginSettings;
    private final CompileTarget compileTarget;
    private final Path cacheDir;
    private final String resourcesProductId;

    ResourcesTask(final JavaPluginSettings pluginSettings,
                  final CompileTarget compileTarget, final Path cacheDir) {

        this.pluginSettings = pluginSettings;
        this.compileTarget = compileTarget;
        this.cacheDir = cacheDir;

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
    public TaskResult run() throws Exception {
        final Optional<ResourcesTreeProduct> resourcesProduct =
            useProduct(resourcesProductId, ResourcesTreeProduct.class);

        if (!resourcesProduct.isPresent()) {
            return TaskResult.empty();
        }

        final Path buildDir =
            Files.createDirectories(resolveBuildDir("resources", compileTarget));

        final Path srcDir = resourcesProduct.get().getSrcDir();

        final KeyValueCache cache = initCache();

        Files.walkFileTree(srcDir, new CopyFileVisitor(buildDir, cache,
            pluginSettings.getResourceFilterGlob(), buildVariablesMap()));

        Files.walkFileTree(buildDir, new DeleteObsoleteFileVisitor(srcDir, cache));

        cache.saveCache();

        return TaskResult.ok(new ProcessedResourceProduct(buildDir));
    }

    private KeyValueCache initCache() {
        if (!getRuntimeConfiguration().isCacheEnabled()) {
            return NULL_CACHE;
        }

        // hash has to change for filter glob changes -- filtered files must not be cached
        final String hash =
            Hasher.hash(List.of(Objects.toString(pluginSettings.getResourceFilterGlob())));

        final Path cacheFile = cacheDir
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

}
