package jobt;

import jobt.api.ProductGraphNode;

public class ProductGraphNodeImpl implements ProductGraphNode {

    private final String productId;

    public ProductGraphNodeImpl(final String productId) {
        this.productId = productId;
    }

    @Override
    public String getProductId() {
        return productId;
    }

}
