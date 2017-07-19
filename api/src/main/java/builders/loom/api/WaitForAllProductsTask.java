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

import builders.loom.api.product.DummyProduct;

public class WaitForAllProductsTask implements Task, ProductDependenciesAware {

    private ProvidedProduct providedProduct;
    private UsedProducts usedProducts;

    @Override
    public void setModule(final Module module) {
        // TODO
    }

    @Override
    public void setProvidedProduct(final ProvidedProduct providedProduct) {
        this.providedProduct = providedProduct;
    }

    @Override
    public void setUsedProducts(final UsedProducts usedProducts) {
        this.usedProducts = usedProducts;
    }

    @Override
    public TaskResult run() throws Exception {
        for (final String productId : usedProducts.getAllowedProductIds()) {
            usedProducts.waitForProduct(productId);
        }

        return new TaskResult(TaskStatus.OK,
            new DummyProduct(providedProduct.getProducedProductId()));
    }

}
