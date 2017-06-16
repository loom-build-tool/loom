package jobt.plugin.mavenresolver;

import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;


public class ProgressLoggingRepositoryListener extends AbstractRepositoryListener {
    @Override
    public void artifactDownloading(final RepositoryEvent event) {

        System.out.println("Start download of artifact " + event.getArtifact() + " from " + event.getRepository().getId() + "...");
    }

    @Override
    public void artifactDownloaded(final RepositoryEvent event) {
        System.out.println("...download of artifcat " + event.getArtifact() + " completed");
    }
}

