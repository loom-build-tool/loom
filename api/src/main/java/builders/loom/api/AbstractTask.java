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

import builders.loom.api.service.ServiceLocator;

public abstract class AbstractTask implements Task,
    ProductDependenciesAware, ServiceLocatorAware {

    private ProvidedProduct providedProduct;
    private UsedProducts usedProducts;
    private ServiceLocator serviceLocator;

    @Override
    public void setProvidedProduct(final ProvidedProduct providedProduct) {
        this.providedProduct = providedProduct;
    }

    public ProvidedProduct getProvidedProduct() {
        return providedProduct;
    }

    @Override
    public void setUsedProducts(final UsedProducts usedProducts) {
        this.usedProducts = usedProducts;
    }

    public UsedProducts getUsedProducts() {
        return usedProducts;
    }

    @Override
    public void setServiceLocator(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

}
