package jobt.plugin.mavenresolver;

import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;


public class ProgressLoggingTransferListener extends AbstractTransferListener {

        @Override
        public void transferStarted(final TransferEvent event) throws TransferCancelledException {
            System.out.println(" start download .. " + event.getResource().getResourceName());
        }

        @Override
        public void transferProgressed(final TransferEvent event) throws TransferCancelledException {
//            System.out.print('\r');
//            System.out.print(" bytes downloaded .. " + event.getTransferredBytes() + "                                                                                            ");
            try {
                Thread.sleep(20);
            } catch (final InterruptedException e) {
            }
        }

        @Override
        public void transferSucceeded(final TransferEvent event) {
            System.out.println("\n success downloaded .. " + event.getTransferredBytes() + "                               ");
        }

}
