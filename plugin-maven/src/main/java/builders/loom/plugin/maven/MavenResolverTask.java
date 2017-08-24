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
import java.util.stream.Collectors;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.api.product.ClasspathProduct;

public class MavenResolverTask extends AbstractModuleTask {

    private final DependencyScope dependencyScope;
    private final MavenResolverPluginSettings pluginSettings;
    private final Path cacheDir;
    private final DownloadProgressEmitter downloadProgressEmitter;

    public MavenResolverTask(final DependencyScope dependencyScope,
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

        return completeOk(resolve(dependencies));
    }

    private List<String> listDependencies() {
        final List<String> deps = new ArrayList<>(getModuleConfig().getCompileDependencies());

        if (dependencyScope == DependencyScope.TEST) {
            deps.addAll(getModuleConfig().getTestDependencies());
        }

        return deps;
    }

    private ClasspathProduct resolve(final List<String> deps) {
        final MavenResolver mavenResolver =
            MavenResolverSingleton.getInstance(pluginSettings, cacheDir, downloadProgressEmitter);

        final List<ArtifactProduct> artifactProducts = mavenResolver.resolve(deps,
            dependencyScope, null);

        final List<Path> collect = artifactProducts.stream()
            .map(ArtifactProduct::getMainArtifact).collect(Collectors.toList());

        return new ClasspathProduct(collect);
    }
}
