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

    private final CompletableFuture<Product> promise = new CompletableFuture<>();

    private final String productId;

    public ProductPromise(final String productId) {
        this.productId = Objects.requireNonNull(productId);
    }

    public void complete(final Product withValue) {
        final boolean completed = promise.complete(withValue);
        if (!completed) {
            throw new IllegalStateException(
                "Product promise <" + productId + "> already completed");
        }
    }

    public String getProductId() {
        return productId;
    }

    public Product getAndWaitForProduct() throws InterruptedException {
        return waitAndGet(promise);
    }

    public boolean isCompleted() {
        return promise.isDone();
    }

    private Product waitAndGet(final Future<Product> future) throws InterruptedException {

        LOG.debug("Requesting product <{}> ...", productId);

        try {
            final Product product = future.get(FUTURE_WAIT_THRESHOLD, TimeUnit.SECONDS);

            LOG.debug("Return product <{}> with value: {}", productId, product);

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

}
