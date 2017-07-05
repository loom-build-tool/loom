package jobt.api.product;

import java.nio.file.Path;

public final class ArtifactProduct {

    private final Path mainArtifact;
    private final Path sourceArtifact;

    public ArtifactProduct(final Path mainArtifact, final Path sourceArtifact) {
        this.mainArtifact = mainArtifact;
        this.sourceArtifact = sourceArtifact;
    }

    public Path getMainArtifact() {
        return mainArtifact;
    }

    public Path getSourceArtifact() {
        return sourceArtifact;
    }

    @Override
    public String toString() {
        return "ArtifactProduct{"
            + "mainArtifact=" + mainArtifact
            + ", sourceArtifact=" + sourceArtifact
            + '}';
    }

}
