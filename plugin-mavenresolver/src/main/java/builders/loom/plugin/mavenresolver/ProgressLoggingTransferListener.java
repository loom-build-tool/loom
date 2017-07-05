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
