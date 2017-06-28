package jobt.api;

public class WaitForAllProductsTask extends AbstractTask {

    @SuppressWarnings("checkstyle:illegalcatch")
    @Override
    public TaskStatus run() throws Exception {

        for (final String productId : getUsedProducts().getAllowedProductIds()) {
            try {
                getUsedProducts().waitForProduct(productId);
            } catch (final Exception e) {
                // do nothing
            }
        }

        return TaskStatus.OK;
    }
}
