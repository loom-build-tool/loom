package builders.loom.plugin.mavenresolver;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import builders.loom.api.product.ArtifactProduct;

public class CachedArtifactProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    private String mainArtifact;
    private String sourceArtifact;

    public CachedArtifactProduct(final String mainArtifact, final String sourceArtifact) {
        this.mainArtifact = mainArtifact;
        this.sourceArtifact = sourceArtifact;
    }

    public String getMainArtifact() {
        return mainArtifact;
    }

    public void setMainArtifact(final String mainArtifact) {
        this.mainArtifact = mainArtifact;
    }

    public String getSourceArtifact() {
        return sourceArtifact;
    }

    public void setSourceArtifact(final String sourceArtifact) {
        this.sourceArtifact = sourceArtifact;
    }

    public static CachedArtifactProduct build(final ArtifactProduct artifactProduct) {
        final String mainArtifact = artifactProduct.getMainArtifact() != null
            ? artifactProduct.getMainArtifact().toString() : null;

        final String sourceArtifact = artifactProduct.getSourceArtifact() != null
            ? artifactProduct.getSourceArtifact().toString() : null;

        return new CachedArtifactProduct(mainArtifact, sourceArtifact);
    }

    public ArtifactProduct buildArtifactProduct() {
        final Path mainArtifactPath = mainArtifact != null ? Paths.get(mainArtifact) : null;
        final Path sourceArtifactPath = sourceArtifact != null ? Paths.get(sourceArtifact) : null;
        return new ArtifactProduct(mainArtifactPath, sourceArtifactPath);
    }

}
