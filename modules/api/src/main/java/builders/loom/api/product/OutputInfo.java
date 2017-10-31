package builders.loom.api.product;

import java.nio.file.Path;
import java.util.Objects;

public final class OutputInfo {

    private final String name;
    private final Path artifact;

    public OutputInfo(final String name) {
        this(name, null);
    }

    public OutputInfo(final String name, final Path artifact) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.artifact = artifact;
    }

    public String getName() {
        return name;
    }

    public Path getArtifact() {
        return artifact;
    }

    @Override
    public String toString() {
        return "OutputInfo{"
            + "name='" + name + '\''
            + ", artifact='" + artifact + '\''
            + '}';
    }

}
