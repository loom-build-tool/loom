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
        LOG.debug("Beginning download of artifact <{}> from {} ...",
            event.getArtifact(), event.getRepository().getId());
        progressIndicator.reportProgress(
            "downloading artifact #" + indexOf(event.getArtifact()) + ": "
                + event.getArtifact());
    }

    @Override
    public void artifactDownloaded(final RepositoryEvent event) {
        LOG.debug("Successfully downloaded artifcat <{}> from {}",
            event.getArtifact(), event.getRepository().getId());
        progressIndicator.reportProgress(
            "just downloaded artifact #" + indexOf(event.getArtifact()) + ": "
                + event.getArtifact());
    }

    private int indexOf(final Artifact artifact) {
        return 1 + inprogressArtifacts.indexOf(artifact.toString());
    }

    private boolean isPom(final Artifact artifact) {
        return "pom".equals(artifact.getExtension());
    }

}
