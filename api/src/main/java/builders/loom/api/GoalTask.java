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

public class GoalTask implements Task, ProductDependenciesAware {

    private final Set<String> usedProductIds;
    private UsedProducts usedProducts;

    public GoalTask(final Set<String> usedProductIds) {
        this.usedProductIds = usedProductIds;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
    }

    @Override
    public void setBuildContext(final BuildContext buildContext) {
    }

    @Override
    public void setUsedProducts(final UsedProducts usedProducts) {
        this.usedProducts = usedProducts;
    }

    @Override
    public TaskResult run() throws Exception {
        for (final String usedProductId : usedProductIds) {
            usedProducts.waitForProduct(usedProductId);
        }

        return new TaskResult(TaskStatus.OK,
            new DummyProduct("goal"));
    }

}
