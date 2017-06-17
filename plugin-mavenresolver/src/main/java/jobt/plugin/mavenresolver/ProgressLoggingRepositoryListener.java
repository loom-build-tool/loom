package jobt.plugin.mavenresolver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.artifact.Artifact;

public class ProgressLoggingRepositoryListener extends AbstractRepositoryListener {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    private final ProgressIndicator progressIndicator;
    private final List<String> inprogressArtifacts = new ArrayList<>();

    public ProgressLoggingRepositoryListener(final ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    @Override
    public void artifactDownloading(final RepositoryEvent event) {
        inprogressArtifacts.add(event.getArtifact().toString());
        LOG.debug("Begin download of artifact <{}> from {} ...",
            event.getArtifact(), event.getRepository().getId());
        progressIndicator.reportProgress("downloading artifact #" + indexOf(event.getArtifact())+": " + event.getArtifact());
    }

    @Override
    public void artifactDownloaded(final RepositoryEvent event) {
        LOG.debug("Successfully downloaded artifcat <{}> from {}",
            event.getArtifact(), event.getRepository().getId());
        progressIndicator.reportProgress("just downloaded artifact #" + indexOf(event.getArtifact()) +": " + event.getArtifact());
    }

    private int indexOf(final Artifact artifact) {
        return 1 + inprogressArtifacts.indexOf(artifact.toString());
    }

    private boolean isPom(final Artifact artifact) {
        return "pom".equals(artifact.getExtension());
    }

}
