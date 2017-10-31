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

package builders.loom.cli;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.core.ModuleRunner;
import builders.loom.core.plugin.ConfiguredTask;

final class ProductReportPrinter {

    private final ModuleRunner moduleRunner;

    ProductReportPrinter(final ModuleRunner moduleRunner) {
        this.moduleRunner = moduleRunner;
    }

    void print(final List<ConfiguredTask> resolvedTasks) {
        // aggregate plugin -> products
        final Map<String, List<ProductInfo>> aggProducts =
            aggregateProducts(resolvedTasks);

        if (!aggProducts.isEmpty()) {
            outputProductInfos(aggProducts);
        }
    }

    private Map<String, List<ProductInfo>> aggregateProducts(
        final Collection<ConfiguredTask> resolvedTasks) {

        // plugin -> products
        final Map<String, List<ProductInfo>> aggProducts = new HashMap<>();

        for (final ConfiguredTask configuredTask : resolvedTasks) {
            final String productId = configuredTask.getProvidedProduct();

            final Optional<Product> product = moduleRunner
                .lookupProduct(configuredTask.getBuildContext(), productId)
                .getWithoutWait();

            if (product.isPresent() && product.get().getOutputInfo().isPresent()) {
                final OutputInfo outputInfo = product.get().getOutputInfo().get();
                final String pluginName = configuredTask.getPluginName();
                aggProducts.putIfAbsent(pluginName, new ArrayList<>());
                aggProducts.get(pluginName).add(new ProductInfo(productId, outputInfo));
            }
        }
        return aggProducts;
    }

    private void outputProductInfos(final Map<String, List<ProductInfo>> aggProducts) {
        AnsiConsole.out().println();

        final List<String> pluginNames = new ArrayList<>(aggProducts.keySet());
        Collections.sort(pluginNames);

        for (final Iterator<String> iterator = pluginNames.iterator(); iterator.hasNext();) {
            final String pluginName = iterator.next();

            final List<ProductInfo> productInfos = aggProducts.get(pluginName);

            final Ansi ansi = Ansi.ansi()
                .bold().a("Products of plugin ")
                .fgBrightBlue().a(pluginName)
                .reset()
                .newline();

            for (final ProductInfo productInfo : productInfos) {
                ansi
                    .bold().a("| ").boldOff()
                    .fgCyan().a(productInfo.getProductId()).fgDefault()
                    .bold().a(" > ").boldOff()
                    .fgGreen().a(productInfo.getOutputInfo().getName());

                if (productInfo.getOutputInfo().getArtifact() != null) {
                    ansi.a(": ").fgDefault().a(Paths.get("").toAbsolutePath().relativize(productInfo.getOutputInfo().getArtifact().toAbsolutePath()).toString());
                }

                ansi.reset().newline();
            }

            if (iterator.hasNext()) {
                ansi.newline();
            }

            AnsiConsole.out().print(ansi);
        }
    }

    private static final class ProductInfo {

        private final String productId;
        private final OutputInfo outputInfo;

        ProductInfo(final String productId, final OutputInfo outputInfo) {
            this.productId = productId;
            this.outputInfo = outputInfo;
        }

        String getProductId() {
            return productId;
        }

        OutputInfo getOutputInfo() {
            return outputInfo;
        }

    }

}
