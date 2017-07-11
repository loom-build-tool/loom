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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.product.Product;

/**
 * Read-only view to products that are required (readProduct) from tasks.
 */
public class UsedProducts {

    private static final Logger LOG = LoggerFactory.getLogger(UsedProducts.class);

    private final ProductRepository productRepository;

    private final Set<String> allowedProductIds;

    public UsedProducts(
        final Set<String> allowedProductIds,
        final ProductRepository productRepository) {
        Objects.requireNonNull(allowedProductIds);
        allowedProductIds.forEach(ProvidedProduct::validateProductIdFormat);
        Objects.requireNonNull(productRepository);
        this.allowedProductIds = Collections.unmodifiableSet(new HashSet<>(allowedProductIds));
        this.productRepository = productRepository;
    }

    public <T extends Product> T readProduct(final String productId, final Class<T> clazz)
        throws InterruptedException {

        Objects.requireNonNull(productId);
        Objects.requireNonNull(clazz);

        final long start = System.nanoTime();

        final ProductPromise productPromise = productRepository.lookup(productId);

        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError(
                "Access to productId <" + productId + "> not configured for task");
        }

        final Object value = productPromise.getAndWaitForProduct();

        final long timeElapsed = (System.nanoTime() - start) / 1_000_000;
        LOG.debug("Blocked for {}ms waiting for product <{}>", timeElapsed, productId);

        return clazz.cast(value);
    }

    public void waitForProduct(final String productId) throws InterruptedException {

        final ProductPromise productPromise = productRepository.lookup(productId);

        if (!allowedProductIds.contains(productId)) {
            throw new IllegalAccessError(
                "Access to productId <" + productId + "> not configured for task");
        }

        productPromise.getAndWaitForProduct();
    }

    public Set<String> getAllowedProductIds() {
        return allowedProductIds;
    }

}
