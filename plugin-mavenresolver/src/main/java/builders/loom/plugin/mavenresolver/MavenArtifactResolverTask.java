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

package builders.loom.plugin.mavenresolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ArtifactListProduct;

public class MavenArtifactResolverTask extends AbstractTask {

    private final DependencyScope dependencyScope;
    private final BuildConfig buildConfig;
    private final MavenResolverPluginSettings pluginSettings;
    private final Path cacheDir;
    private MavenResolver mavenResolver;

    public MavenArtifactResolverTask(final DependencyScope dependencyScope,
                                     final BuildConfig buildConfig,
                                     final MavenResolverPluginSettings pluginSettings,
                                     final Path cacheDir) {

        this.dependencyScope = dependencyScope;
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
        this.cacheDir = cacheDir;
    }

    @Override
    public TaskResult run() throws Exception {
        this.mavenResolver = MavenResolverSingleton.getInstance(pluginSettings, cacheDir);
        switch (dependencyScope) {
            case COMPILE:
                return completeOk(productCompile());
            case TEST:
                return completeOk(productTest());
            default:
                throw new IllegalStateException("Unknown scope: " + dependencyScope);
        }
    }

    private ArtifactListProduct productCompile() {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        return new ArtifactListProduct(mavenResolver.resolve(dependencies, DependencyScope.COMPILE,
            "sources"));
    }

    private ArtifactListProduct productTest() {
        final List<String> dependencies = new ArrayList<>(buildConfig.getDependencies());
        dependencies.addAll(buildConfig.getTestDependencies());

        return new ArtifactListProduct(mavenResolver.resolve(dependencies, DependencyScope.TEST,
            "sources"));
    }

}
