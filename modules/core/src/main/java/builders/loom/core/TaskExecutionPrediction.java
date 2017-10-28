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

package builders.loom.core;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.LoomPaths;
import builders.loom.api.ProductPromise;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.UsedProducts;
import builders.loom.api.product.Product;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.util.Hasher;
import builders.loom.util.Iterables;

public class TaskExecutionPrediction {

    private static final Logger LOG = LoggerFactory.getLogger(TaskExecutionPrediction.class);

    private final RuntimeConfiguration runtimeConfiguration;
    private final ConfiguredTask configuredTask;
    private final UsedProducts usedProducts;
    private final String signature;

    public TaskExecutionPrediction(final RuntimeConfiguration runtimeConfiguration,
                                   final ConfiguredTask configuredTask,
                                   final UsedProducts usedProducts) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.configuredTask = configuredTask;
        this.usedProducts = usedProducts;

        try {
            signature = calcSignature();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e); // TODO ???
        }
    }

    public String calcSignature() throws InterruptedException {
        final List<String> skipHints = configuredTask.getSkipHints();

        // Prevent skipping tasks by default
        if (skipHints.isEmpty() && !configuredTask.isGoal()) {
            LOG.debug("No skip hints configured -- don't skip");
            return "PREVENT-SKIP:" + UUID.randomUUID().toString();
        }

        final List<String> checksumParts = new ArrayList<>(skipHints);

        // guarantee stable order
        final List<ProductPromise> orderedProductPromises = usedProducts.getAllProducts().stream()
            .sorted(Comparator
                .comparing(ProductPromise::getModuleName)
                .thenComparing(ProductPromise::getProductId))
            .collect(Collectors.toList());

        for (final ProductPromise productPromise : orderedProductPromises) {
            final String productId = productPromise.getProductId();
            final String moduleName = productPromise.getModuleName();

            final String checksum = usedProducts
                .readProduct(moduleName, productId, Product.class)
                .map(Product::checksum)
                .orElse("EMPTY");

            checksumParts.add(String.format("%s#%s:%s", moduleName, productId, checksum));
        }

        final String hash = Hasher.hash(checksumParts);

        LOG.debug("Signature: {}; Hash: {}", checksumParts, hash);

        return hash;
    }

    public boolean canSkipTask() {
        final Path checksumFile = buildFileName(".sig"); // TODO better suffix
        if (Files.notExists(checksumFile)) {
            return false;
        }

        try {
            final String signatureLastRun =
                Iterables.getOnlyElement(Files.readAllLines(checksumFile)); // TODO improve

            final boolean equals = signatureLastRun.equals(signature);
            LOG.info("Last run: {} - current: {}; equals: {}", signatureLastRun, signature, equals);
            return equals;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private Path buildFileName(final String suffix) {
        return LoomPaths.loomDir(runtimeConfiguration.getProjectBaseDir())
            .resolve(Paths.get(Version.getVersion(), "checksums",
                configuredTask.getBuildContext().getModuleName(),
                configuredTask.getProvidedProduct() + suffix));
    }

    public void clearSignature() {
        try {
            Files.deleteIfExists(buildFileName(".sig"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void commitSignature() {
        final Path checksumFile = buildFileName(".sig"); // TODO better suffix

        try {
            Files.write(checksumFile, List.of(signature), StandardOpenOption.CREATE_NEW); // TODO improve
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
