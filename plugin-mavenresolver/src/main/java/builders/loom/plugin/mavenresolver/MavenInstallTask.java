package builders.loom.plugin.mavenresolver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.Project;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.DummyProduct;

public class MavenInstallTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final LocalRepositoryManager localRepositoryManager;
    private final RepositorySystem system;

    public MavenInstallTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;

        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        system = locator.getService(RepositorySystem.class);

        final LocalRepository localRepo = new LocalRepository(findLocalMavenRepository().toFile());
        localRepositoryManager = system.newLocalRepositoryManager(localRepo);
    }

    private Path findLocalMavenRepository() {
        // TODO read settings.xml for localRepository configuration
        return Paths.get(System.getProperty("user.home"), ".m2", "repository");
    }

    @Override
    public TaskResult run() throws Exception {
        final AssemblyProduct jarProduct = requireProduct("jar", AssemblyProduct.class);
        final Path jarFile = jarProduct.getAssemblyFile();

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(localRepositoryManager);

        final Path tmpPomFile = Files.createTempFile("pom", null);

        try {
            final Project project = buildConfig.getProject();
            writePom(tmpPomFile, project);

            final InstallRequest request = new InstallRequest();
            request
                .addArtifact(buildArtifact(project, "jar", jarFile))
                .addArtifact(buildArtifact(project, "pom", tmpPomFile));

            system.install(session, request);
        } finally {
            Files.delete(tmpPomFile);
        }

        return completeOk(new DummyProduct(""));
    }

    private void writePom(final Path tmpPomFile, final Project project) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        try (final OutputStream out = newOut(tmpPomFile)) {
            writer.write(out, buildModel(project));
        }
    }

    private BufferedOutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.APPEND));
    }

    private Model buildModel(final Project project) {
        final Model pom = new Model();
        pom.setModelVersion("4.0.0");
        pom.setGroupId(project.getGroupId());
        pom.setArtifactId(project.getArtifactId());
        pom.setVersion(project.getVersion());

        for (final String compileDependency : buildConfig.getDependencies()) {
            pom.addDependency(mapDependency(compileDependency, null));
        }

        for (final String testDependency : buildConfig.getTestDependencies()) {
            pom.addDependency(mapDependency(testDependency, "test"));
        }

        return pom;
    }

    private Dependency mapDependency(final String compileDependency, final String scope) {
        final DefaultArtifact defaultArtifact = new DefaultArtifact(compileDependency);
        final Dependency dependency = new Dependency();
        dependency.setGroupId(defaultArtifact.getGroupId());
        dependency.setArtifactId(defaultArtifact.getArtifactId());
        dependency.setVersion(defaultArtifact.getVersion());
        dependency.setScope(scope);
        return dependency;
    }

    private DefaultArtifact buildArtifact(final Project project, final String extension,
                                          final Path assemblyFile) {
        return new DefaultArtifact(project.getGroupId(), project.getArtifactId(), null,
            extension, project.getVersion(), null, assemblyFile.toFile());
    }

}
