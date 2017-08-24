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

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import builders.loom.api.product.ArtifactProduct;

public final class CachedArtifactProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String mainArtifact;
    private final String sourceArtifact;

    private CachedArtifactProduct(final String mainArtifact, final String sourceArtifact) {
        this.mainArtifact = mainArtifact;
        this.sourceArtifact = sourceArtifact;
    }

    public static CachedArtifactProduct build(final ArtifactProduct artifactProduct) {
        final String mainArtifact = artifactProduct.getMainArtifact() != null
            ? artifactProduct.getMainArtifact().toString() : null;

        final String sourceArtifact = artifactProduct.getSourceArtifact() != null
            ? artifactProduct.getSourceArtifact().toString() : null;

        return new CachedArtifactProduct(mainArtifact, sourceArtifact);
    }

    ArtifactProduct buildArtifactProduct() {
        final Path mainArtifactPath = mainArtifact != null ? Paths.get(mainArtifact) : null;
        final Path sourceArtifactPath = sourceArtifact != null ? Paths.get(sourceArtifact) : null;
        return new ArtifactProduct(mainArtifactPath, sourceArtifactPath);
    }

}
