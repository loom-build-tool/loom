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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.transfer.AbstractTransferListener;
import org.sonatype.aether.transfer.TransferCancelledException;
import org.sonatype.aether.transfer.TransferEvent;

import builders.loom.api.DownloadProgressEmitter;

public class ProgressLoggingTransferListener extends AbstractTransferListener {

    private static final Logger LOG =
        LoggerFactory.getLogger(ProgressLoggingTransferListener.class);

    private final DownloadProgressEmitter downloadProgressEmitter;

    ProgressLoggingTransferListener(final DownloadProgressEmitter downloadProgressEmitter) {
        this.downloadProgressEmitter = downloadProgressEmitter;
    }

    @Override
    public void transferStarted(final TransferEvent event) throws TransferCancelledException {
        downloadProgressEmitter.progressFiles();
    }

    @Override
    public void transferProgressed(final TransferEvent event) throws TransferCancelledException {
        downloadProgressEmitter.progressBytes(event.getDataLength());
    }

    @Override
    public void transferSucceeded(final TransferEvent event) {
        LOG.debug("Downloaded {} bytes in {}ms from <{}>",
            event.getTransferredBytes(),
            System.currentTimeMillis() - event.getResource().getTransferStartTime(),
            event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

}
