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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.product.Product;

/**
 * Read-only view to products that are required (readProduct) from tasks.
 */
public class UsedProducts {

    private static final Logger LOG = LoggerFactory.getLogger(UsedProducts.class);

    private final String selfModuleName;
    private final Set<ProductPromise> productPromises;
    private final Set<ProductPromise> actuallyWaitedForProducts = new CopyOnWriteArraySet<>();

    public UsedProducts(final String selfModuleName, final Set<ProductPromise> productPromises) {
        this.selfModuleName = selfModuleName;
        this.productPromises = productPromises;
    }

    public <T extends Product> Optional<T> readProduct(final String productId, final Class<T> clazz)
        throws InterruptedException {

        return readProduct(selfModuleName, productId, clazz);
    }

    public <T extends Product> Optional<T> readProduct(
        final String moduleName, final String productId, final Class<T> clazz)
        throws InterruptedException {

        Objects.requireNonNull(productId);
        Objects.requireNonNull(clazz);

        final long start = System.nanoTime();

        final Optional<Product> value = getAndWaitProduct(moduleName, productId);

        final long timeElapsed = (System.nanoTime() - start) / 1_000_000;
        LOG.debug("Blocked for {}ms waiting for product <{}>", timeElapsed, productId);

        return value.map(clazz::cast);
    }

    public void waitForOptionalProduct(final String productId) throws InterruptedException {
        final Optional<ProductPromise> optProductPromise =
            lookupProductPromise(selfModuleName, productId);

        if (optProductPromise.isPresent()) {
            final ProductPromise productPromise = optProductPromise.get();
            actuallyWaitedForProducts.add(productPromise);
            productPromise.getAndWaitForProduct();
        }
    }

    public void waitForProduct(final String productId) throws InterruptedException {
        getAndWaitProduct(selfModuleName, productId);
    }

    public Optional<Product> getAndWaitProduct(final String moduleName, final String productId)
        throws InterruptedException {

        final ProductPromise productPromise = lookupProductPromise(moduleName, productId)
            .orElseThrow(()
                -> new IllegalStateException(
                String.format("No access to product <%s> of module <%s>",
                    productId, moduleName)));

        actuallyWaitedForProducts.add(productPromise);
        return productPromise.getAndWaitForProduct();
    }

    private Optional<ProductPromise> lookupProductPromise(final String moduleName,
                                                          final String productId) {
        return productPromises.stream()
            .filter(pp -> pp.getModuleName().equals(moduleName)
                && pp.getProductId().equals(productId))
            .findFirst();
    }

    public Set<ProductPromise> getAllProducts() {
        return Collections.unmodifiableSet(productPromises);
    }

    /**
     * Value is only useful after tasks have been running.
     */
    public Set<ProductPromise> getActuallyUsedProducts() {
        return Collections.unmodifiableSet(actuallyWaitedForProducts);
    }

}
