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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.DirectoryProduct;
import builders.loom.util.TempFile;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class MavenInstallTask extends AbstractModuleTask {

    private final MavenPluginSettings pluginSettings;
    private final LocalRepositoryManager localRepositoryManager;
    private final RepositorySystem system;

    public MavenInstallTask(final MavenPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;

        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(VersionResolver.class, DefaultVersionResolver.class);
        locator.addService(VersionRangeResolver.class, DefaultVersionRangeResolver.class);
        locator.addService(ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class);
        system = locator.getService(RepositorySystem.class);

        final LocalRepository localRepo = new LocalRepository(
            new MavenSettingsHelper().findLocalMavenRepository().toFile());
        localRepositoryManager = system.newLocalRepositoryManager(localRepo);
    }

    @Override
    public TaskResult run() throws Exception {
        if (getRuntimeConfiguration().getVersion() == null) {
            throw new IllegalStateException("Artifact version required "
                + "(specify --artifact-version)");
        }

        if (pluginSettings.getGroupAndArtifact() == null) {
            // Not every module needs to be installed
            return TaskResult.empty();
        }

        final Path jarFile = requireProduct("jar", AssemblyProduct.class).getAssemblyFile();

        return TaskResult.ok(new DirectoryProduct(install(jarFile),
            "Directory of installed artifact"));
    }

    private Path install(final Path jarFile) throws IOException, InstallationException {
        final AtomicReference<Path> installPath = new AtomicReference<>();

        final MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(localRepositoryManager);
        session.setRepositoryListener(new AbstractRepositoryListener() {
            @Override
            public void artifactInstalled(final RepositoryEvent event) {
                // We only have to set the parent path of .jar -- pom is the same...
                installPath.compareAndSet(null, event.getFile().toPath().getParent());
            }
        });

        final Path tmpDir = Files.createDirectories(
            LoomPaths.tmpDir(getRuntimeConfiguration().getProjectBaseDir()));
        try (final TempFile tmpPomFile = new TempFile(tmpDir, "pom", null)) {
            final Artifact jarArtifact = buildArtifact(jarFile);
            writePom(buildModel(jarArtifact), tmpPomFile.getFile());

            final InstallRequest request = new InstallRequest();
            final SubArtifact pomArtifact = new SubArtifact(jarArtifact, null, "pom",
                tmpPomFile.getFile().toFile());
            request
                .addArtifact(jarArtifact)
                .addArtifact(pomArtifact);

            system.install(session, request);
        }

        return installPath.get();
    }

    private void writePom(final Model model, final Path tmpPomFile) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        try (final OutputStream out = newOut(tmpPomFile)) {
            writer.write(out, model);
        }
    }

    private BufferedOutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.APPEND));
    }

    private Model buildModel(final Artifact artifact) {
        final Model pom = new Model();
        pom.setModelVersion("4.0.0");
        pom.setGroupId(artifact.getGroupId());
        pom.setArtifactId(artifact.getArtifactId());
        pom.setVersion(artifact.getVersion());

        for (final String compileDependency : getModuleConfig().getCompileDependencies()) {
            pom.addDependency(mapDependency(compileDependency, null));
        }

        for (final String testDependency : getModuleConfig().getTestDependencies()) {
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

    private Artifact buildArtifact(final Path assemblyFile) {
        final String version = getRuntimeConfiguration().getVersion();

        return new DefaultArtifact(
            String.format("%s:%s", pluginSettings.getGroupAndArtifact(), version))
            .setFile(assemblyFile.toFile());
    }

}
