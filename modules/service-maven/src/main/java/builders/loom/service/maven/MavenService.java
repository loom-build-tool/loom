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

package builders.loom.service.maven;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.service.ResolvedArtifact;

public class MavenService implements DependencyResolverService {

    private static final String REPOSITORY_URL = "https://repo.maven.apache.org/maven2/";

    private RuntimeConfiguration runtimeConfiguration;
    private Path repositoryPath;
    private DownloadProgressEmitter downloadProgressEmitter;
    private DependencyResolver dependencyResolver;

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    @Override
    public void setRepositoryPath(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public void setDownloadProgressEmitter(final DownloadProgressEmitter downloadProgressEmitter) {
        this.downloadProgressEmitter = downloadProgressEmitter;
    }

    @Override
    public void init() {
        dependencyResolver = runtimeConfiguration.isCacheEnabled()
            ? new CachingMavenResolver(REPOSITORY_URL, downloadProgressEmitter, repositoryPath)
            : new NoCacheMavenResolver(REPOSITORY_URL, downloadProgressEmitter);
    }

    @Override
    public List<Path> resolveMainArtifacts(final List<String> deps, final List<String> excludes,
                                           final DependencyScope scope) {
        return resolveArtifacts(deps, excludes, scope, false).stream()
            .map(ResolvedArtifact::getMainArtifact)
            .collect(Collectors.toList());
    }

    @Override
    public List<ResolvedArtifact> resolveArtifacts(final List<String> deps,
                                                   final List<String> excludes,
                                                   final DependencyScope scope,
                                                   final boolean withSources) {
        return dependencyResolver.resolve(deps, excludes, scope, withSources);
    }

}
