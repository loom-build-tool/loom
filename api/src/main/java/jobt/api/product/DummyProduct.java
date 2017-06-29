package jobt.api.product;

import java.util.Objects;

public final class DummyProduct implements Product {

    private final String text;

    /**
     * Provide a justification.
     */
    public DummyProduct(final String text) {
        Objects.requireNonNull(text);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }

}
