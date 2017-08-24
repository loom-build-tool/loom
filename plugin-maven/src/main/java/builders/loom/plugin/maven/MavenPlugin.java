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
import java.util.stream.Collectors;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.product.ArtifactProduct;

public class MavenPlugin extends AbstractPlugin<MavenResolverPluginSettings> {

    public MavenPlugin() {
        super(new MavenResolverPluginSettings());
    }

    @Override
    public void configure() {
        final MavenResolverPluginSettings pluginSettings = getPluginSettings();
        final Path repositoryPath = getRepositoryPath();
        final DownloadProgressEmitter downloadProgressEmitter = getDownloadProgressEmitter();

        final CachingMavenResolver cachingMavenResolver =
            new CachingMavenResolver(pluginSettings, downloadProgressEmitter, repositoryPath);

        task("resolveCompileDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.COMPILE, cachingMavenResolver))
            .provides("compileDependencies", true)
            .desc("Fetches dependencies needed for main class compilation.")
            .register();

        task("resolveCompileArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.COMPILE,
                cachingMavenResolver))
            .provides("compileArtifacts", true)
            .desc("Fetches compile dependencies (incl. sources) needed for IDE import.")
            .register();

        task("resolveTestDependencies")
            .impl(() -> new MavenResolverTask(DependencyScope.TEST, cachingMavenResolver))
            .provides("testDependencies", true)
            .desc("Fetches dependencies needed for test class compilation.")
            .register();

        task("resolveTestArtifacts")
            .impl(() -> new MavenArtifactResolverTask(DependencyScope.TEST,
                cachingMavenResolver))
            .provides("testArtifacts", true)
            .desc("Fetches test dependencies (incl. sources) needed for IDE import.")
            .register();

        task("install")
            .impl(() -> new MavenInstallTask(pluginSettings))
            .provides("mavenArtifact")
            .uses("jar")
            .desc("Installs the jar file to the local Maven repository.")
            .register();

        service("mavenDependencyResolver")
            .impl(() -> (DependencyResolverService) (deps, scope, cacheName) ->
                cachingMavenResolver
                    .resolve(deps, scope, null).stream()
                    .map(ArtifactProduct::getMainArtifact)
                    .collect(Collectors.toList()))
            .register();
    }

}
