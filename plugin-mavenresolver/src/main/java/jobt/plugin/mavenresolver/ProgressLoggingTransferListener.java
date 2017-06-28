package jobt.plugin.mavenresolver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;

public class ProgressLoggingTransferListener extends AbstractTransferListener {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);
    private final ProgressIndicator progressIndicator;

    public ProgressLoggingTransferListener(final ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    @Override
    public void transferStarted(final TransferEvent event) throws TransferCancelledException {
        progressIndicator.reportProgress(
            "downloading resource " + event.getResource().getResourceName());
    }

    @Override
    public void transferProgressed(final TransferEvent event) throws TransferCancelledException {
        progressIndicator.reportProgress(
            "downloaded " + event.getTransferredBytes() + " bytes  for "
                + event.getResource().getResourceName());
    }

    @Override
    public void transferSucceeded(final TransferEvent event) {
        progressIndicator.reportProgress(
            "finished downloading " + event.getTransferredBytes()
            + " bytes  for " + event.getResource().getResourceName());

        LOG.debug("Loaded {} bytes in {}ms from <{}>",
            event.getTransferredBytes(),
            System.currentTimeMillis() - event.getResource().getTransferStartTime(),
            event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

    private boolean isPom(final File file) {
        return file.getName().endsWith(".pom");
    }

}
