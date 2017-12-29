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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.service.ResolvedArtifact;
import builders.loom.util.Hasher;
import builders.loom.util.serialize.Record;
import builders.loom.util.serialize.SimpleSerializer;

public class CachingMavenResolver implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(CachingMavenResolver.class);

    private final String repositoryUrl;
    private final DownloadProgressEmitter downloadProgressEmitter;
    private final Path cacheDir;

    CachingMavenResolver(final String repositoryUrl,
                         final DownloadProgressEmitter downloadProgressEmitter,
                         final Path cacheDir) {
        this.repositoryUrl = repositoryUrl;
        this.downloadProgressEmitter = downloadProgressEmitter;
        this.cacheDir = cacheDir;
    }

    @Override
    public List<ResolvedArtifact> resolve(final List<String> deps, final List<String> excludes,
                                          final DependencyScope scope, final boolean withSources) {

        final Hasher hasher = new Hasher().putStrings(deps);

        if (excludes != null) {
            hasher.putStrings(excludes);
        }

        final Path cacheFile = cacheDir.resolve(
            String.format("dependencies-%s-%s-%s",
                scope.name().toLowerCase(),
                withSources ? "with-sources" : "wo-sources",
                hasher.hashHex()));

        // note: caches do not need extra locking, because they get isolated by the scope used
        final Optional<List<ResolvedArtifact>> cachedArtifacts = readCache(cacheFile);
        if (cachedArtifacts.isPresent()) {
            final List<ResolvedArtifact> artifacts = cachedArtifacts.get();
            LOG.debug("Resolved {} dependencies {} to {} from cache", scope, deps, artifacts);
            return artifacts;
        }

        final List<ResolvedArtifact> artifacts = MavenResolverSingleton
            .getInstance(repositoryUrl, downloadProgressEmitter)
            .resolve(deps, excludes, scope, withSources);

        LOG.debug("Resolved {} dependencies {} to {}", scope, deps, artifacts);

        writeCache(artifacts, cacheFile);

        return artifacts;
    }

    private Optional<List<ResolvedArtifact>> readCache(final Path file) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }

        final List<ResolvedArtifact> artifacts = new ArrayList<>();

        try {
            SimpleSerializer.read(file, record -> {
                final List<String> fields = record.getFields();
                final Path mainArtifact = Paths.get(fields.get(0));
                final Path sourceArtifact = fields.get(1) != null ? Paths.get(fields.get(1)) : null;
                final ResolvedArtifact artifact =
                    new ResolvedArtifactImpl(mainArtifact, sourceArtifact);
                artifacts.add(artifact);
            });
        } catch (final IOException e) {
            LOG.debug("Corrupt cache", e);
            return Optional.empty();
        }

        return Optional.of(artifacts);
    }

    private void writeCache(final List<ResolvedArtifact> artifacts, final Path file) {
        try {
            Files.createDirectories(file.getParent());
            SimpleSerializer.write(file, artifacts, CachingMavenResolver::mapRecord);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Record mapRecord(final ResolvedArtifact artifact) {
        final String mainArtifact = artifact.getMainArtifact().toAbsolutePath().toString();
        final String sourceArtifact = artifact.getSourceArtifact() != null
            ? artifact.getSourceArtifact().toAbsolutePath().toString()
            : null;
        return new Record(mainArtifact, sourceArtifact);
    }

}
