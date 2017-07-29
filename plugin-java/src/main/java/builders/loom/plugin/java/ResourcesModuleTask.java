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
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ResourcesTreeProduct;

public class ResourcesModuleTask extends AbstractModuleTask {

    private final JavaPluginSettings pluginSettings;
    private final Path destPath;
    private final CompileTarget compileTarget;
    private final Path cacheDir;

    ResourcesModuleTask(final JavaPluginSettings pluginSettings,
                        final CompileTarget compileTarget, final Path cacheDir) {

        this.pluginSettings = pluginSettings;
        this.compileTarget = compileTarget;

        destPath = Paths.get("build", "resources", compileTarget.name().toLowerCase());

        this.cacheDir = cacheDir;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<ResourcesTreeProduct> resourcesProduct = getResourcesTreeProduct();

        if (!resourcesProduct.isPresent()) {
            if (Files.exists(destPath)) {
                FileUtil.deleteDirectoryRecursively(destPath, true);
            }
            return completeSkip();
        }

        final Path srcPath = resourcesProduct.get().getSrcDir();

        assertDirectoryOrMissing(srcPath);
        assertDirectoryOrMissing(destPath);

        final RuntimeConfiguration runtimeConfiguration = getRuntimeConfiguration();

        final KeyValueCache cache = runtimeConfiguration.isCacheEnabled()
            ? new DiskKeyValueCache(getCacheFileName())
            : new NullKeyValueCache();

        final Map<String, String> variables = new HashMap<>();
        //variables.put("project.groupId", buildConfig.getProject().getGroupId());
        //variables.put("project.artifactId", buildConfig.getProject().getArtifactId());
        variables.put("project.version", runtimeConfiguration.getVersion());

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath, cache,
            pluginSettings.getResourceFilterGlob(), variables));

        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath, cache));

        cache.saveCache();

        return completeOk(new ProcessedResourceProduct(destPath));
    }

    private Optional<ResourcesTreeProduct> getResourcesTreeProduct() throws InterruptedException {
        switch (compileTarget) {
            case MAIN:
                return useProduct("resources", ResourcesTreeProduct.class);
            case TEST:
                return useProduct("testResources", ResourcesTreeProduct.class);
            default:
                throw new IllegalStateException();
        }
    }

    private Path getCacheFileName() {
        return cacheDir.resolve("resource-" + compileTarget.name().toLowerCase() + ".cache");
    }

    private void assertDirectoryOrMissing(final Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Path '" + path + "' is not a directory");
        }
    }

}
