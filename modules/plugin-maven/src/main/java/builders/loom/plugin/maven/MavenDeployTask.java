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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.api.product.UnmanagedGenericProduct;
import builders.loom.util.Preconditions;
import builders.loom.util.TempFile;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
// TODO de-duplicate code (MavenInstallTask)
public class MavenDeployTask extends AbstractModuleTask {

    private final MavenPluginSettings pluginSettings;
    private final LocalRepositoryManager localRepositoryManager;
    private final RepositorySystem system;

    public MavenDeployTask(final MavenPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;

        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        locator.setServices(WagonProvider.class, new DefaultWagonProvider());
        system = locator.getService(RepositorySystem.class);

        final LocalRepository localRepo = new LocalRepository(
            new MavenSettingsHelper().findLocalMavenRepository().toFile());
        localRepositoryManager = system.newLocalRepositoryManager(localRepo);
    }

    @Override
    public TaskResult run() throws Exception {
        if (getRuntimeConfiguration().getVersion() == null) {
            throw new IllegalStateException("Artifact version required "
                + "(specify --release)");
        }

        if (pluginSettings.getGroupAndArtifact() == null) {
            throw new IllegalStateException("Missing configuration of maven.groupAndArtifact");
        }

        final Path jarFile = Paths.get(requireProduct("jar", Product.class)
            .getProperty("classesJarFile"));

        final Path sourceJarFile = Paths.get(requireProduct("sourcesJar", Product.class)
            .getProperty("sourceJarFile"));

        final Path javadocJarFile = Paths.get(requireProduct("javadocJar", Product.class)
            .getProperty("javaDocJarFile"));

        final String url = deploy(jarFile, sourceJarFile, javadocJarFile);

        return TaskResult.done(newProduct(url));
    }

    private String deploy(final Path jarFile, final Path sourceJarFile, final Path javadocFile)
        throws IOException, RepositoryException {

        final String url;

        final LocalRepository localRepo = new LocalRepository(
            new MavenSettingsHelper().findLocalMavenRepository().toFile());
        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo)); // FIXME

        final Path tmpDir = Files.createDirectories(
            LoomPaths.tmpDir(getRuntimeConfiguration().getProjectBaseDir()));

        final Artifact jarArtifact = buildArtifact(jarFile);

        final Path templatePom =
            getBuildContext().getPath().resolve("loom-pom.xml");

        final Model model = readTemplateModel(templatePom);
        enhanceModel(model, jarArtifact);

        // TODO show error if file is missing
        final DeployProperties deployProperties = readDeployProperties();


        try (final TempFile tmpPomFile = new TempFile(tmpDir, "pom", null);
             final TempFile tmpJarSig = new TempFile(tmpDir, "jar", "asc");
             final TempFile tmpJarSourceSig = new TempFile(tmpDir, "sourcesJar", "asc");
             final TempFile tmpJarDocSig = new TempFile(tmpDir, "javadocJar", "asc");
             final TempFile tmpPomSig = new TempFile(tmpDir, "pom", "asc")) {

            writeTmpPom(model, tmpPomFile.getFile());

            final DeployRequest request = new DeployRequest();

            url = jarArtifact.isSnapshot()
                ? deployProperties.getSnapshotUrl()
                : deployProperties.getReleaseUrl();
            final RemoteRepository remoteRepository = new RemoteRepository(null, "default", url);

            if (deployProperties.getUsername() != null) {
                remoteRepository.setAuthentication(new Authentication(
                    deployProperties.getUsername(), deployProperties.getPassword()));
            }

            final SubArtifact sourceArtifact = subArtifact(sourceJarFile, jarArtifact, "*-sources");

            final SubArtifact javaDocArtifact = subArtifact(javadocFile, jarArtifact, "*-javadoc");

            final SubArtifact pomArtifact = new SubArtifact(jarArtifact, null, "pom",
                tmpPomFile.getFile().toFile());

            request
                .setRepository(remoteRepository)
                .addArtifact(jarArtifact)
                .addArtifact(sourceArtifact)
                .addArtifact(javaDocArtifact)
                .addArtifact(pomArtifact);

            if (deployProperties.isSigningEnabled()) {
                final Signer signer = new Signer(deployProperties.getKeyRingFile(),
                    deployProperties.getKeyPassword(), deployProperties.getKeyId());

                request
                    .addArtifact(signedArtifact(jarArtifact, signer, tmpJarSig))
                    .addArtifact(signedArtifact(sourceArtifact, signer, tmpJarSourceSig))
                    .addArtifact(signedArtifact(javaDocArtifact, signer, tmpJarDocSig))
                    .addArtifact(signedArtifact(pomArtifact, signer, tmpPomSig));
            }

            system.deploy(session, request);
        }

        return url;
    }

    private SubArtifact subArtifact(final Path sourceJarFile, final Artifact jarArtifact,
                                    final String classifier) {
        return new SubArtifact(jarArtifact, classifier, "jar",
            sourceJarFile.toFile());
    }

    private SubArtifact signedArtifact(final Artifact jarArtifact, final Signer signer,
                                       final TempFile tmpJarSig) {
        signer.sign(jarArtifact.getFile().toPath(), tmpJarSig.getFile());
        return new SubArtifact(jarArtifact, jarArtifact.getClassifier(), "*.asc",
            tmpJarSig.getFile().toFile());
    }

    private DeployProperties readDeployProperties() throws IOException {
        final Path deployConfig = LoomPaths.configDir(getRuntimeConfiguration().getProjectBaseDir())
            .resolve("loom-deploy.properties");

        Preconditions.checkState(
            Files.exists(deployConfig),
            "Missing configuration file: %s", deployConfig);

        // TODO validate properties
        return new DeployProperties(deployConfig);
    }

    private Model readTemplateModel(final Path templatePom) throws IOException {
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        try (InputStream in = Files.newInputStream(templatePom)) {
            try {
                return reader.read(in);
            } catch (final XmlPullParserException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void enhanceModel(final Model pom, final Artifact artifact) {
        pom.setGroupId(artifact.getGroupId());
        pom.setArtifactId(artifact.getArtifactId());
        pom.setVersion(artifact.getVersion());

        for (final String compileDependency : getModuleConfig().getCompileDependencies()) {
            pom.addDependency(mapDependency(compileDependency, null));
        }

        for (final String testDependency : getModuleConfig().getTestDependencies()) {
            pom.addDependency(mapDependency(testDependency, "test"));
        }
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

    private Artifact buildArtifact(final Path assemblyFile) {
        final String version = getRuntimeConfiguration().getVersion();

        return new DefaultArtifact(
            String.format("%s:%s", pluginSettings.getGroupAndArtifact(), version))
            .setFile(assemblyFile.toFile());
    }

    private static Product newProduct(final String url) {
        return new UnmanagedGenericProduct(Collections.emptyMap(),
            new OutputInfo("Deployed to " + url));
    }

    private void writeTmpPom(final Model model, final Path tmpPomFile) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        try (final OutputStream out = newOut(tmpPomFile)) {
            writer.write(out, model);
        }
    }

    private BufferedOutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.APPEND));
    }
}
