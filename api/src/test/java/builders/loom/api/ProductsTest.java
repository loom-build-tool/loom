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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

import builders.loom.api.product.AbstractProduct;

public class ProductsTest {

    private final ProductRepository productRepository = new ProductRepositoryDummy();

    @Test
    public void successProvideAndUse() throws InterruptedException {

        final ProvidedProduct providedProduct = provides("a");

        final UsedProducts usedProducts = uses("a");

        providedProduct.complete("a", new StringProduct("foo"));

        assertEquals("foo", usedProducts.readProduct("a", StringProduct.class)
            .get().toString());

    }

    @Test
    public void failDoubleComplete() {

        final ProvidedProduct providedProduct = provides("a");

        providedProduct.complete("a", new StringProduct("result"));

        try {
            providedProduct.complete("a", new StringProduct("result-double"));
            fail();
        } catch (final IllegalStateException e) {
            assertEquals("Product promise <a> already completed", e.getMessage());
        }

    }

    @Test
    public void failCompleteWithNull() {

        final ProvidedProduct providedProduct = provides("a");

        try {
            providedProduct.complete("a", null);
        } catch (final NullPointerException e) {
            assertEquals("Must not complete product <a> in task <sampleTask> with null value",
                e.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidFormat() {

        provides("ZZZ");

    }

    @Test(expected = IllegalStateException.class)
    public void failOnMultipleProvides() {

        provides("x");
        provides("x");

    }

    @Test
    public void failOnUnknownProduct() throws InterruptedException {

        try {
            final UsedProducts uses = uses("x");
            uses.readProduct("x", StringProduct.class);
            fail();
        } catch (final NullPointerException npe) {
            assertEquals("No product found by id <x>", npe.getMessage());
        }

    }

    private UsedProducts uses(final String... productIdLists) {
        return new UsedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), productRepository);
    }

    private ProvidedProduct provides(final String productId) {
        productRepository.createProduct(productId);
        return new ProvidedProduct(productId, productRepository, "sampleTask");
    }

    private static class ProductRepositoryDummy implements ProductRepository {

        private final Map<String, ProductPromise> products = new HashMap<>();

        @Override
        public ProductPromise lookup(final String productId) {
            return Objects.requireNonNull(
                products.get(productId),
                "No product found by id <" + productId + ">");
        }

        @Override
        public void createProduct(final String productId) {
            final ProductPromise old = products.putIfAbsent(
                productId, new ProductPromise(productId));
            if (old != null) {
                throw new IllegalStateException();
            }
        }
    }

    static class StringProduct extends AbstractProduct {

        private final String str;

        StringProduct(final String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
