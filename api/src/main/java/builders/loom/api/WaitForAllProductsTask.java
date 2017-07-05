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

import java.util.Set;

import builders.loom.api.product.DummyProduct;

public class WaitForAllProductsTask extends AbstractTask {

    @Override
    public TaskStatus run() throws Exception {
        final ProvidedProducts providedProducts = getProvidedProducts();
        final Set<String> producedProductIds = providedProducts.getProducedProductIds();

        if (producedProductIds.size() != 1) {
            throw new IllegalStateException("WaitForAllProductsTask accepts only exact 1 product");
        }

        final UsedProducts usedProducts = getUsedProducts();
        for (final String productId : usedProducts.getAllowedProductIds()) {
            usedProducts.waitForProduct(productId);
        }

        final String productId = producedProductIds.iterator().next();
        providedProducts.complete(productId, new DummyProduct(productId));

        return TaskStatus.OK;
    }

}
