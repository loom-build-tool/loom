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

import java.util.List;

import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.service.ResolvedArtifact;

public class NoCacheMavenResolver implements DependencyResolver {

    private final String repositoryUrl;
    private final DownloadProgressEmitter downloadProgressEmitter;

    NoCacheMavenResolver(final String repositoryUrl,
                         final DownloadProgressEmitter downloadProgressEmitter) {
        this.repositoryUrl = repositoryUrl;
        this.downloadProgressEmitter = downloadProgressEmitter;
    }

    @Override
    public List<ResolvedArtifact> resolve(final List<String> deps, final DependencyScope scope,
                                          final boolean withSources) {
        return MavenResolverSingleton
            .getInstance(repositoryUrl, downloadProgressEmitter)
            .resolve(deps, scope, withSources);
    }

}
