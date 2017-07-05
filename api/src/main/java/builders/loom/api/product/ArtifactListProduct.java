package builders.loom.api.product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArtifactListProduct implements Product {

    private final List<ArtifactProduct> artifacts;

    public ArtifactListProduct(final List<ArtifactProduct> artifacts) {
        this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
    }

    public List<ArtifactProduct> getArtifacts() {
        return artifacts;
    }

}
