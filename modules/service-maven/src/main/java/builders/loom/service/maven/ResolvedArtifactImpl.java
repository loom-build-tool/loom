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

package builders.loom.service.maven;

import java.nio.file.Path;

import builders.loom.api.service.ResolvedArtifact;

public class ResolvedArtifactImpl implements ResolvedArtifact {

    private final Path mainArtifact;
    private final Path sourceArtifact;

    public ResolvedArtifactImpl(final Path mainArtifact, final Path sourceArtifact) {
        this.mainArtifact = mainArtifact;
        this.sourceArtifact = sourceArtifact;
    }

    @Override
    public Path getMainArtifact() {
        return mainArtifact;
    }

    @Override
    public Path getSourceArtifact() {
        return sourceArtifact;
    }

    @Override
    public String toString() {
        return "ResolvedArtifactImpl{"
            + "mainArtifact=" + mainArtifact
            + ", sourceArtifact=" + sourceArtifact
            + '}';
    }

}
