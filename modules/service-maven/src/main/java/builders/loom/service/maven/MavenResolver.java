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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import builders.loom.api.DependencyScope;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.service.ResolvedArtifact;
import builders.loom.util.SystemUtil;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class MavenResolver implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    private final RepositorySystem system;
    private final RemoteRepository mavenRepository;
    private final LocalRepositoryManager localRepositoryManager;
    private final ProgressLoggingTransferListener transferListener;

    MavenResolver(final String repositoryUrl,
                  final DownloadProgressEmitter downloadProgressEmitter) {

        LOG.debug("Initialize MavenResolver");
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.setServices(WagonProvider.class, new DefaultWagonProvider());

        system = locator.getService(RepositorySystem.class);

        mavenRepository =
            new RemoteRepository("central", "default", repositoryUrl);

        final Path repository = SystemUtil.determineLoomBaseDir().resolve("repository");
        final LocalRepository localRepo = new LocalRepository(repository.toFile());
        localRepositoryManager = system.newLocalRepositoryManager(localRepo);

        transferListener = new ProgressLoggingTransferListener(downloadProgressEmitter);

        LOG.debug("MavenResolver initialized");
    }

    @Override
    public List<ResolvedArtifact> resolve(final List<String> deps, final DependencyScope scope,
                                          final boolean withSources) {

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(localRepositoryManager);
        session.setTransferListener(transferListener);

        final List<RemoteRepository> repositories = Collections.singletonList(mavenRepository);

        final CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(repositories);
        collectRequest.setDependencies(deps.stream()
            .map(dep -> new Dependency(new DefaultArtifact(dep), mavenScope(scope)))
            .collect(Collectors.toList())
        );

        final List<ResolvedArtifact> ret = new ArrayList<>();

        try {
            final DependencyNode node =
                system.collectDependencies(session, collectRequest).getRoot();

            final DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);

            final DependencyResult dependencyResult =
                system.resolveDependencies(session, dependencyRequest);

            final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);

            final List<ArtifactResult> artifactResults = dependencyResult.getArtifactResults();

            if (withSources) {
                for (final ArtifactResult artifactResult : artifactResults) {
                    ret.add(resolveArtifactWithSource(scope, session, repositories,
                        artifactResult));
                }
            } else {
                for (final ArtifactResult artifactResult : artifactResults) {
                    ret.add(resolveArtifact(artifactResult));
                }
            }

            return ret;
        } catch (final DependencyCollectionException | DependencyResolutionException e) {
            throw new IllegalStateException(
                String.format("Unresolvable dependencies for scope <%s>: %s",
                    scope, e.getMessage()), e);
        }
    }

    private ResolvedArtifact resolveArtifact(final ArtifactResult artifactResult) {
        final Path mainArtifact = artifactResult.getArtifact().getFile().toPath();
        return new ResolvedArtifactImpl(mainArtifact, null);
    }

    private ResolvedArtifact resolveArtifactWithSource(final DependencyScope scope,
                                                       final MavenRepositorySystemSession session,
                                                       final List<RemoteRepository> repositories,
                                                       final ArtifactResult artifactResult) {

        final Path mainArtifactFile = artifactResult.getArtifact().getFile().toPath();

        final Artifact sourceArtifact =
            new SubArtifact(artifactResult.getArtifact(), "sources", "jar");
        final ArtifactRequest sourceArtifactReq =
            new ArtifactRequest(sourceArtifact, repositories, mavenScope(scope));

        Path sourceArtifactFile = null;

        try {
            final ArtifactResult sourceArtifactRes =
                system.resolveArtifact(session, sourceArtifactReq);

            sourceArtifactFile = sourceArtifactRes.getArtifact().getFile().toPath();
        } catch (final ArtifactResolutionException e) {
            // not all artifacts have sources attached to
            LOG.debug("Couldn't fetch source artifact for {}", sourceArtifact, e);
        }

        return new ResolvedArtifactImpl(mainArtifactFile, sourceArtifactFile);
    }

    private static String mavenScope(final DependencyScope scope) {
        switch (scope) {
            case COMPILE:
                return "compile";
            case TEST:
                return "test";
            default:
                throw new IllegalStateException("Unknown scope: " + scope);
        }
    }

}
