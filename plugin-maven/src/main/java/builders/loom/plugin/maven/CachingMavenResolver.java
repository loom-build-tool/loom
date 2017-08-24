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

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.product.ArtifactProduct;
import builders.loom.util.Hasher;

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
        if (Files.exists(cacheFile)) {
            final List<ArtifactProduct> artifacts = readCache(cacheFile);
            LOG.debug("Resolved {} dependencies {} to {} from cache", scope, deps, artifacts);
            return artifacts;
        }

        final List<ArtifactProduct> artifacts = MavenResolverSingleton
            .getInstance(pluginSettings, downloadProgressEmitter)
            .resolve(deps, scope, classifier);

        LOG.debug("Resolved {} dependencies {} to {}", scope, deps, artifacts);

        writeCache(artifacts, cacheFile);

        return artifacts;
    }

    private List<ArtifactProduct> readCache(final Path file) {
        final List<ArtifactProduct> artifacts = new ArrayList<>();

        try (final ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            while (true) {
                try {
                    final Path mainArtifact = Paths.get((String) in.readObject());
                    final Object o2 = in.readObject();
                    final Path sourceArtifact = o2 != null ? Paths.get((String) o2) : null;
                    artifacts.add(new ArtifactProduct(mainArtifact, sourceArtifact));
                } catch (final EOFException e) {
                    break;
                }
            }

            return artifacts;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeCache(final List<ArtifactProduct> artifacts, final Path file) {
        try {
            Files.createDirectories(file.getParent());

            final Path tmpFile = Paths.get(file.toString() + ".tmp");

            try (final ObjectOutputStream out =
                     new ObjectOutputStream(Files.newOutputStream(tmpFile))) {
                for (final ArtifactProduct artifact : artifacts) {
                    out.writeObject(artifact.getMainArtifact().toAbsolutePath().toString());

                    if (artifact.getSourceArtifact() != null) {
                        out.writeObject(artifact.getSourceArtifact().toAbsolutePath().toString());
                    } else {
                        out.writeObject(null);
                    }
                }
            }

            Files.move(tmpFile, file, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
