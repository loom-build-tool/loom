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

package builders.loom.plugin.mavenresolver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import builders.loom.api.*;
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
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;

import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.DirectoryProduct;
import builders.loom.util.TempFile;

@SuppressWarnings({"checkstyle:classdataabstractioncoupling", "checkstyle:classfanoutcomplexity"})
public class MavenInstallTask extends AbstractTask {

    private final BuildConfig buildConfig;
    private final MavenResolverPluginSettings pluginSettings;
    private final RuntimeConfiguration runtimeConfiguration;
    private final LocalRepositoryManager localRepositoryManager;
    private final RepositorySystem system;

    public MavenInstallTask(final BuildConfig buildConfig, final MavenResolverPluginSettings pluginSettings,
                            final RuntimeConfiguration runtimeConfiguration) {
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
        this.runtimeConfiguration = runtimeConfiguration;

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
        final AssemblyProduct jarProduct = requireProduct("jar", AssemblyProduct.class);
        final Path jarFile = jarProduct.getAssemblyFile();

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

        try (final TempFile tmpPomFile = new TempFile("pom", null)) {
            writePom(tmpPomFile.getFile());

            final InstallRequest request = new InstallRequest();
            final DefaultArtifact jarArtifact = buildArtifact("jar", jarFile);
            final SubArtifact pomArtifact = new SubArtifact(jarArtifact, null, "pom",
                tmpPomFile.getFile().toFile());
            request
                .addArtifact(jarArtifact)
                .addArtifact(pomArtifact);

            system.install(session, request);
        }

        return completeOk(new DirectoryProduct(installPath.get(),
            "Directory of installed artifact"));
    }

    private void writePom(final Path tmpPomFile) throws IOException {
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        try (final OutputStream out = newOut(tmpPomFile)) {
            writer.write(out, buildModel());
        }
    }

    private BufferedOutputStream newOut(final Path file) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.APPEND));
    }

    private Model buildModel() {
        final Model pom = new Model();
        pom.setModelVersion("4.0.0");
        pom.setGroupId(pluginSettings.getGroupId());
        pom.setArtifactId(pluginSettings.getArtifactId());
        pom.setVersion(runtimeConfiguration.getVersion());

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

    private DefaultArtifact buildArtifact(final String extension, final Path assemblyFile) {
        return new DefaultArtifact(pluginSettings.getGroupId(), pluginSettings.getArtifactId(), null,
            extension, runtimeConfiguration.getVersion(), null, assemblyFile.toFile());
    }

}
