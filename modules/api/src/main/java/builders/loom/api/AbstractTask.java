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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.product.Product;

public abstract class AbstractTask implements Task,
    ProductDependenciesAware {

    @SuppressWarnings("checkstyle:visibilitymodifier")
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private RuntimeConfiguration runtimeConfiguration;
    private BuildContext buildContext;
    private UsedProducts usedProducts;

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public BuildContext getBuildContext() {
        return buildContext;
    }

    @Override
    public void setBuildContext(final BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @Override
    public void setUsedProducts(final UsedProducts usedProducts) {
        this.usedProducts = usedProducts;
    }

    public UsedProducts getUsedProducts() {
        return usedProducts;
    }

    public <P extends Product> P requireProduct(final String productId, final Class<P> productClass)
        throws InterruptedException {

        return useProduct(productId, productClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format("Requested product <%s> is not present", productId)));
    }

    public <P extends Product> P requireProduct(
        final String moduleName, final String productId, final Class<P> productClass)
        throws InterruptedException {

        return useProduct(moduleName, productId, productClass)
            .orElseThrow(() -> new IllegalStateException(
                String.format(
                    "Requested product <%s> of module <%s> is not present",
                    productId, moduleName)));
    }

    public <P extends Product> Optional<P> useProduct(final String productId,
                                                      final Class<P> productClass)
        throws InterruptedException {
        Objects.requireNonNull(productId, "productId required");
        Objects.requireNonNull(productClass, "productClass required");
        return usedProducts.readProduct(productId, productClass);
    }

    public <P extends Product> Optional<P> useProduct(
        final String moduleName, final String productId, final Class<P> productClass)
        throws InterruptedException {
        Objects.requireNonNull(moduleName, "moduleName required");
        Objects.requireNonNull(productId, "productId required");
        Objects.requireNonNull(productClass, "productClass required");
        return usedProducts.readProduct(moduleName, productId, productClass);
    }

}
