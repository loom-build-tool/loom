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
import java.util.stream.Collectors;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.BuildConfig;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.product.ArtifactProduct;

public class MavenResolverPlugin extends AbstractPlugin<MavenResolverPluginSettings> {

    public MavenResolverPlugin() {
        super(new MavenResolverPluginSettings());
    }

    @Override
    public void configure() {
        final BuildConfig cfg = getBuildConfig();
        final MavenResolverPluginSettings pluginSettings = getPluginSettings();
        final Path repositoryPath = getRepositoryPath();

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, cfg, pluginSettings,
                repositoryPath))
            .provides("compileDependencies", true)
            .desc("Fetches dependencies needed for main class compilation.")
            .register();

        task("resolveCompileArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.COMPILE, cfg, pluginSettings,
                repositoryPath))
            .provides("compileArtifacts", true)
            .desc("Fetches compile dependencies (incl. sources) needed for IDE import.")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, cfg, pluginSettings,
                repositoryPath))
            .provides("testDependencies", true)
            .desc("Fetches dependencies needed for test class compilation.")
            .register();

        task("resolveTestArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.TEST, cfg, pluginSettings,
                repositoryPath))
            .provides("testArtifacts", true)
            .desc("Fetches test dependencies (incl. sources) needed for IDE import.")
            .register();

        task("install")
            .impl(() -> new MavenInstallTask(getBuildConfig()))
            .provides("mavenArtifact")
            .uses("jar")
            .desc("Installs the jar file to the local Maven repository.")
            .register();

        service("mavenDependencyResolver")
            .impl(() -> (DependencyResolverService) (deps, scope, cacheName) ->
                MavenResolverSingleton.getInstance(pluginSettings, repositoryPath)
                    .resolve(deps, scope, null).stream()
                    .map(ArtifactProduct::getMainArtifact)
                    .collect(Collectors.toList()))
            .register();
    }

}
