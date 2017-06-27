package jobt.plugin.java;

import jobt.api.AbstractTask;
import jobt.api.TaskStatus;

public class WaitForAllProductsTask extends AbstractTask {

    @Override
    public void prepare() throws Exception {
    }

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
