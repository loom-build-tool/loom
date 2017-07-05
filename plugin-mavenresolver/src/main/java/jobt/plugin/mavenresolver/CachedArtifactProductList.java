package jobt.plugin.mavenresolver;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import jobt.api.product.ArtifactProduct;

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
