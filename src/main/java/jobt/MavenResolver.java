package jobt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
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
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import jobt.api.DependencyResolver;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class MavenResolver implements DependencyResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    private final RepositorySystem system;
    private final RemoteRepository mavenRepository;
    private LocalRepositoryManager localRepositoryManager;

    public MavenResolver() {
        LOG.debug("Initialize MavenResolver");
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.setServices(WagonProvider.class, new WagonProvider() {
            @Override
            public Wagon lookup(final String roleHint) throws Exception {
                if ("http".equals(roleHint)) {
                    final LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();
                    lightweightHttpWagon.setAuthenticator(new LightweightHttpWagonAuthenticator());
                    return lightweightHttpWagon;
                }
                return null;
            }

            @Override
            public void release(final Wagon wagon) {
            }
        });

        system = locator.getService(RepositorySystem.class);

        mavenRepository =
            new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");

        final Path repository = Paths.get(System.getProperty("user.home"), ".jobt", "repository");
        final LocalRepository localRepo = new LocalRepository(repository.toFile());
        localRepositoryManager = system.newLocalRepositoryManager(localRepo);
        LOG.debug("MavenResolver initialized");
    }

    @Override
    public List<Path> buildClasspath(final List<String> deps, final String scope) {
        LOG.info("Resolve {} dependencies: {}", scope, deps);

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(localRepositoryManager);

        final PreorderNodeListGenerator nlg;
        try {
            final List<Path> files = readCache(deps, scope);

            if (!files.isEmpty()) {
                return files;
            }

            final CollectRequest collectRequest = new CollectRequest();

            final List<Dependency> dependencies = deps.stream()
                .map(a -> new Dependency(new DefaultArtifact(a), scope))
                .collect(Collectors.toList());

            collectRequest.setDependencies(dependencies);
            collectRequest.addRepository(mavenRepository);
            final DependencyNode node =
                system.collectDependencies(session, collectRequest).getRoot();

            final DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);

            system.resolveDependencies(session, dependencyRequest);

            nlg = new PreorderNodeListGenerator();
            node.accept(nlg);

            buildHash(deps, scope, nlg);
        } catch (final IOException | DependencyCollectionException
            | DependencyResolutionException e) {
            throw new IllegalStateException(e);
        }

        final List<Path> libs = nlg.getFiles().stream()
            .map(File::toPath)
            .collect(Collectors.toList());

        LOG.debug("Resolved {} dependencies {} to {}", scope, deps, libs);
        return libs;
    }

    private List<Path> readCache(final List<String> deps, final String scope) throws IOException {
        List<Path> files = new ArrayList<>();

        final Path path = Paths.get(".jobt", scope + "-dependencies");
        if (Files.notExists(path)) {
            return files;
        }

        final List<String> strings = Files.readAllLines(path);
        final String[] split = strings.get(0).split("\t");

        final String hash = Hasher.hash(deps);
        if (hash.equals(split[0])) {
            final String[] pathNames = split[1].split(",");
            files = Arrays.stream(pathNames).map(Paths::get).collect(Collectors.toList());
        }

        return files;
    }

    private void buildHash(final List<String> deps, final String scope,
                           final PreorderNodeListGenerator nlg) throws IOException {
        final Path buildDir = Paths.get(".jobt");
        if (Files.notExists(buildDir)) {
            Files.createDirectories(buildDir);
        }

        final String sb = Hasher.hash(deps) + '\t' + nlg.getFiles().stream()
            .map(File::getAbsolutePath)
            .collect(Collectors.joining(","));

        Files.write(Paths.get(".jobt", scope + "-dependencies"), Collections.singletonList(sb),
            StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
    }

}
