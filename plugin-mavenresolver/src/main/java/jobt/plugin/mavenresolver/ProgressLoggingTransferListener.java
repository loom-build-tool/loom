package jobt.plugin.mavenresolver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;

public class ProgressLoggingTransferListener extends AbstractTransferListener {

    private static final Logger LOG = LoggerFactory.getLogger(MavenResolver.class);

    @Override
    public void transferStarted(final TransferEvent event) throws TransferCancelledException {
    }

    @Override
    public void transferProgressed(final TransferEvent event) throws TransferCancelledException {
    }

    @Override
    public void transferSucceeded(final TransferEvent event) {
        if (isPom(event.getResource().getFile())) {
            return;
        }
        LOG.debug("Loaded {} bytes in {}ms from <{}>",
            event.getTransferredBytes(),
            System.currentTimeMillis() - event.getResource().getTransferStartTime(),
            event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

    private boolean isPom(final File file) {
        return file.getName().endsWith(".pom");
    }

}
