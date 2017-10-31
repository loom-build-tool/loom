package builders.loom.api.product;

import java.util.Objects;

public final class OutputInfo {

    private final String name;
    private final String details;

    public OutputInfo(final String name) {
        this(name, null);
    }

    public OutputInfo(final String name, final String details) {
        this.name = Objects.requireNonNull(name, "name is required");
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "OutputInfo{"
            + "name='" + name + '\''
            + ", details='" + details + '\''
            + '}';
    }

}
