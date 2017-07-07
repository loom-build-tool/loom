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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.product.Product;

/**
 * Sink for provided products from upstream task.
 */
public class ProvidedProducts {

    public static final Pattern PATTERN = Pattern.compile("[a-z][a-zA-Z.]*");

    private static final Logger LOG = LoggerFactory.getLogger(ProvidedProducts.class);

    private final ProductRepository productRepository;

    private final Set<String> producedProductIds;

    private final String taskName;

    public ProvidedProducts(final Set<String> producedProductIds,
                            final ProductRepository productRepository, final String taskName) {
        Objects.requireNonNull(taskName);
        this.taskName = taskName;
        Objects.requireNonNull(producedProductIds);
        producedProductIds.forEach(ProvidedProducts::validateProductIdFormat);
        Objects.requireNonNull(productRepository);

        this.producedProductIds = Collections.unmodifiableSet(new HashSet<>(producedProductIds));
        this.productRepository = productRepository;
    }

    public <T extends Product> void complete(final String productId, final T value) {
        Objects.requireNonNull(productId);
        Objects.requireNonNull(value,
            "Must not complete product <" + productId + ">"
                + " in task <" + taskName + "> with null value");

        if (!producedProductIds.contains(productId)) {
            throw new IllegalStateException(
                "Not allowed to resolve productId <" + productId + "> in task <" + taskName + ">");
        }

        final ProductPromise productPromise = productRepository.lookup(productId);

        if (productPromise.isCompleted()) {
            throw new IllegalStateException(
                String.format("Task <%s> has tried to complete the already completed product <%s>",
                    taskName, productId));
        }

        productPromise.complete(value);

        LOG.debug("Product promise <{}> completed by task <{}> with value (type {}): {}",
            productId, value.getClass().getSimpleName(), value);
    }

    public Set<String> getProducedProductIds() {
        return producedProductIds;
    }

    public static void validateProductIdFormat(final String id) {
        if (!ProvidedProducts.PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid format of product id <%s>", id));
        }
    }
}
