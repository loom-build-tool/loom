/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.api;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.product.Product;

public final class ProductPromise {

    private static final Logger LOG = LoggerFactory.getLogger(ProductPromise.class);

    private static final int FUTURE_WAIT_THRESHOLD = 10;

    private final String moduleName;
    private final String productId;

    private final CompletableFuture<Optional<Product>> promise = new CompletableFuture<>();

    // TODO startTimeAt
    private long startTime;
    private long completedAt;

    public ProductPromise(final String moduleName, final String productId) {
        this.moduleName = Objects.requireNonNull(moduleName);
        this.productId = Objects.requireNonNull(productId);
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    public void complete(final Product withValue) {
        final boolean completed = promise.complete(Optional.ofNullable(withValue));
        if (!completed) {
            throw new IllegalStateException(
                "Product promise <" + productId + "> already completed");
        }


        final long newValue = System.nanoTime();

        if (newValue < startTime) {
            throw new IllegalStateException();
        }

        completedAt = newValue;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getProductId() {
        return productId;
    }

    public Optional<Product> getAndWaitForProduct() throws InterruptedException {
        return waitAndGet(promise);
    }

    public Optional<Product> getWithoutWait() {
        return promise.getNow(Optional.empty());
    }

    private Optional<Product> waitAndGet(final Future<Optional<Product>> future)
        throws InterruptedException {

        LOG.debug("Requesting product <{}> ...", productId);

        try {
            final Optional<Product> product = future.get(FUTURE_WAIT_THRESHOLD, TimeUnit.SECONDS);

            LOG.debug("Return product <{}> with value: {}", productId, product.orElse(null));

            return product;
        } catch (final ExecutionException e) {
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {

            LOG.warn("Blocked for {} seconds waiting for product <{}> - continue",
                FUTURE_WAIT_THRESHOLD, productId);

            try {
                return future.get();
            } catch (final ExecutionException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getCompletedAt() {
        return completedAt;
    }

}
