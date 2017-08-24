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

package builders.loom.plugin.maven;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;
import builders.loom.api.product.ArtifactProduct;

public class MavenArtifactResolverTask extends AbstractModuleTask {

    private final DependencyScope dependencyScope;
    private final MavenResolverPluginSettings pluginSettings;
    private final Path cacheDir;
    private final DownloadProgressEmitter downloadProgressEmitter;

    public MavenArtifactResolverTask(final DependencyScope dependencyScope,
                                     final MavenResolverPluginSettings pluginSettings,
                                     final Path cacheDir,
                                     final DownloadProgressEmitter downloadProgressEmitter) {

        this.dependencyScope = dependencyScope;
        this.pluginSettings = pluginSettings;
        this.cacheDir = cacheDir;
        this.downloadProgressEmitter = downloadProgressEmitter;
    }

    @Override
    public TaskResult run() throws Exception {
        final List<String> dependencies = listDependencies();

        if (dependencies.isEmpty()) {
            return completeEmpty();
        }

        return completeOk(new ArtifactListProduct(resolve(dependencies)));
    }

    private List<String> listDependencies() {
        final List<String> deps = new ArrayList<>(getModuleConfig().getCompileDependencies());

        switch (dependencyScope) {
            case COMPILE:
                break;
            case TEST:
                deps.addAll(getModuleConfig().getTestDependencies());
                break;
            default:
                throw new IllegalStateException("Unknown scope: " + dependencyScope);
        }

        return deps;
    }

    private List<ArtifactProduct> resolve(final List<String> dependencies) {
        final MavenResolver mavenResolver =
            MavenResolverSingleton.getInstance(pluginSettings, cacheDir, downloadProgressEmitter);

        return mavenResolver.resolve(dependencies, dependencyScope, "sources");
    }

}
