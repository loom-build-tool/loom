package jobt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
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
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

public class MavenResolver {

    public String buildClasspath(final List<String> deps, final String scope)
        throws DependencyCollectionException, DependencyResolutionException {

        System.out.println("Resolve dependencies for scope " + scope);

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


        final RepositorySystem system = locator.getService(RepositorySystem.class);

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();

        final Path repository = Paths.get(System.getProperty("user.home"), ".jobt", "repository");

        final LocalRepository localRepo = new LocalRepository(repository.toFile());
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

        final RemoteRepository mavenRepository =
            new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");

        final CollectRequest collectRequest = new CollectRequest();

        final List<Dependency> dependencies = deps.stream()
            .map(a -> new Dependency(new DefaultArtifact(a), scope))
            .collect(Collectors.toList());

        collectRequest.setDependencies(dependencies);
        collectRequest.addRepository(mavenRepository);
        final DependencyNode node = system.collectDependencies(session, collectRequest).getRoot();

        final DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setRoot(node);

        system.resolveDependencies(session, dependencyRequest);

        final PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        return nlg.getClassPath();
    }

}
