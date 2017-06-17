package jobt.plugin.mavenresolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.artifact.Artifact;

public class ProgressLoggingRepositoryListener extends AbstractRepositoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    @Override
    public void artifactDownloading(final RepositoryEvent event) {
        if (isPom(event.getArtifact())) {
            return;
        }
        LOG.debug("Begin download of artifact <{}> from {} ...",
            event.getArtifact(), event.getRepository().getId());
    }

    @Override
    public void artifactDownloaded(final RepositoryEvent event) {
        if (isPom(event.getArtifact())) {
            return;
        }
        LOG.debug("Successfully downloaded artifcat <{}> from {}",
            event.getArtifact(), event.getRepository().getId());
    }

    private boolean isPom(final Artifact artifact) {
        return "pom".equals(artifact.getExtension());
    }

}
