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

package builders.loom.core.plugin;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import builders.loom.api.ProductPromise;
import builders.loom.api.ProductRepository;

public class ProductRepositoryImpl implements ProductRepository {

    private final Map<String, ProductPromise> products = new ConcurrentHashMap<>();

    @Override
    public ProductPromise lookup(final String productId) {
        return
            Objects.requireNonNull(
                products.get(productId), "No product found by id <" + productId + ">");
    }

    @Override
    public void createProduct(final String moduleName, final String productId) {

        final ProductPromise oldValue =
            products.putIfAbsent(productId, new ProductPromise(moduleName, productId));

        if (oldValue != null) {
            throw new IllegalStateException(
                "Product <" + productId + "> already registered");
        }

    }

}
