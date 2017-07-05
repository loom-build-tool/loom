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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import builders.loom.api.product.ArtifactProduct;

public class CachedArtifactProductList implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CachedArtifactProduct> artifactProducts;

    public CachedArtifactProductList() {
    }

    public CachedArtifactProductList(final List<CachedArtifactProduct> artifactProducts) {
        this.artifactProducts = artifactProducts;
    }

    public List<CachedArtifactProduct> getArtifactProducts() {
        return artifactProducts;
    }

    public void setArtifactProducts(final List<CachedArtifactProduct> artifactProducts) {
        this.artifactProducts = artifactProducts;
    }

    public static CachedArtifactProductList build(final List<ArtifactProduct> artifacts) {
        final CachedArtifactProductList list = new CachedArtifactProductList();
        list.setArtifactProducts(artifacts.stream()
            .map(CachedArtifactProduct::build)
            .collect(Collectors.toList()));

        return list;
    }

    public List<ArtifactProduct> buildArtifactProductList() {
        return artifactProducts.stream()
            .map(CachedArtifactProduct::buildArtifactProduct)
            .collect(Collectors.toList());
    }

}
