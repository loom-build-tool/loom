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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.util.Hasher;
import builders.loom.util.SerializationUtil;

public class CachingMavenResolver implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(CachingMavenResolver.class);

    private final MavenResolverPluginSettings pluginSettings;
    private final DownloadProgressEmitter downloadProgressEmitter;
    private final Path cacheDir;

    public CachingMavenResolver(final MavenResolverPluginSettings pluginSettings,
                                final DownloadProgressEmitter downloadProgressEmitter,
                                final Path cacheDir) {
        this.pluginSettings = pluginSettings;
        this.downloadProgressEmitter = downloadProgressEmitter;
        this.cacheDir = cacheDir;
    }

    @Override
    public List<ArtifactProduct> resolve(final List<String> deps, final DependencyScope scope,
                                         final String classifier) {

        final Path cacheFile = cacheDir.resolve(
            String.format("dependencies-%s-%s-%s",
                scope.name().toLowerCase(),
                Objects.toString(classifier, "unclassified"),
                Hasher.hash(deps)));

        // note: caches do not need extra locking, because they get isolated by the scope used
        final CachedArtifactProductList cachedArtifacts =
            (CachedArtifactProductList) SerializationUtil.readCache(cacheFile);

        if (cachedArtifacts != null) {
            final List<ArtifactProduct> artifacts = cachedArtifacts.buildArtifactProductList();
            LOG.debug("Resolved {} dependencies {} to {} from cache", scope, deps, artifacts);
            return Collections.unmodifiableList(artifacts);
        }

        final MavenResolver mavenResolver =
            MavenResolverSingleton.getInstance(pluginSettings, downloadProgressEmitter);

        final List<ArtifactProduct> artifacts = mavenResolver.resolve(deps, scope, classifier);
        LOG.debug("Resolved {} dependencies {} to {}", scope, deps, artifacts);

        SerializationUtil.write(CachedArtifactProductList.build(artifacts), cacheFile);

        return artifacts;
    }

}
