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

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.CompileTarget;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.api.product.ResourcesTreeProduct;

public class ResourcesTask extends AbstractTask {

    private final RuntimeConfiguration runtimeConfiguration;
    private final BuildConfig buildConfig;
    private final JavaPluginSettings pluginSettings;
    private final Path destPath;
    private final CompileTarget compileTarget;
    private final Path cacheDir;

    ResourcesTask(final RuntimeConfiguration runtimeConfiguration,
                  final BuildConfig buildConfig,
                  final JavaPluginSettings pluginSettings,
                  final CompileTarget compileTarget, final Path cacheDir) {

        this.runtimeConfiguration = runtimeConfiguration;
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
        this.compileTarget = compileTarget;

        destPath = Paths.get("loombuild", "resources", compileTarget.name().toLowerCase());

        this.cacheDir = cacheDir;
    }

    @Override
    public TaskStatus run() throws Exception {

        final Path srcPath;

        switch (compileTarget) {
            case MAIN:
                srcPath = getUsedProducts().readProduct(
                    "resources", ResourcesTreeProduct.class).getSrcDir();
                break;
            case TEST:
                srcPath = getUsedProducts().readProduct(
                    "testResources", ResourcesTreeProduct.class).getSrcDir();
                break;
            default:
                throw new IllegalStateException();
        }

        if (Files.notExists(srcPath) && Files.notExists(destPath)) {
            return complete(TaskStatus.SKIP);
        }

        assertDirectoryOrMissing(srcPath);
        assertDirectoryOrMissing(destPath);

        if (Files.notExists(srcPath) && Files.exists(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath, true);
            return complete(TaskStatus.OK);
        }

        FileUtil.assertDirectory(srcPath);

        final KeyValueCache cache = runtimeConfiguration.isCacheEnabled()
            ? new DiskKeyValueCache(getCacheFileName())
            : new NullKeyValueCache();

        final Map<String, String> variables = new HashMap<>();
        variables.put("project.groupId", buildConfig.getProject().getGroupId());
        variables.put("project.artifactId", buildConfig.getProject().getArtifactId());
        variables.put("project.version", buildConfig.getProject().getVersion());

        Files.walkFileTree(srcPath, new CopyFileVisitor(destPath, cache,
            pluginSettings.getResourceFilterGlob(), variables));

        Files.walkFileTree(destPath, new DeleteObsoleteFileVisitor(srcPath, cache));

        cache.saveCache();

        return complete(TaskStatus.OK);
    }

    private Path getCacheFileName() {
        return cacheDir.resolve("resource-" + compileTarget.name().toLowerCase() + ".cache");
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProduct().complete(
                    "processedResources", new ProcessedResourceProduct(destPath));
                return status;
            case TEST:
                getProvidedProduct().complete(
                    "processedTestResources", new ProcessedResourceProduct(destPath));
                return status;
            default:
                throw new IllegalStateException();
        }
    }

    private void assertDirectoryOrMissing(final Path path) {
        if (Files.exists(path) && !Files.isDirectory(path)) {
            throw new IllegalStateException("Path '" + path + "' is not a directory");
        }
    }

}
