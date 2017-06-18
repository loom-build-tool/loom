package jobt.api;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ProductPromise {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPromise.class);

    private static final int FUTURE_WAIT_THRESHOLD = 30;

    private final CompletableFuture<Object> promise = new CompletableFuture<>();

    private final String productId;

    public ProductPromise(final String productId) {
        this.productId = Objects.requireNonNull(productId);
    }

    public void complete(final Object withValue) {
        final boolean completed = promise.complete(withValue);
        if (!completed) {
            throw new IllegalStateException("Future <"+productId+"> already completed!");
        }
    }

    public Object get() {
        return waitAndGet(promise);
    }

    private Object waitAndGet(final Future<Object> future) {
        try {
            return future.get(FUTURE_WAIT_THRESHOLD, TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {
            try {
                LOG.warn("Already waiting for {} seconds for <{}> - continue",
                    FUTURE_WAIT_THRESHOLD, productId);
                return future.get();
            } catch (final ExecutionException e1) {
                throw new IllegalStateException(e1);
            } catch (final InterruptedException e1) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e1);
            }
        }
    }

}
