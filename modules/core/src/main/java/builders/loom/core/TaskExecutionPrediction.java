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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.LoomPaths;
import builders.loom.api.ProductPromise;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.UsedProducts;
import builders.loom.api.product.ManagedProduct;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.util.FileUtil;
import builders.loom.util.Hashing;
import builders.loom.util.SkipChecksumUtil;

class TaskExecutionPrediction {

    private static final Logger LOG = LoggerFactory.getLogger(TaskExecutionPrediction.class);

    private final RuntimeConfiguration runtimeConfiguration;
    private final ConfiguredTask configuredTask;
    private final UsedProducts usedProducts;
    private final String signature;
    private final Path sigFile;

    TaskExecutionPrediction(final RuntimeConfiguration runtimeConfiguration,
                            final ConfiguredTask configuredTask,
                            final UsedProducts usedProducts) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.configuredTask = configuredTask;
        this.usedProducts = usedProducts;
        signature = calcSignature();
        sigFile = buildFileName();
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    private String calcSignature() {
        final List<String> skipHints;
        try {
            skipHints = configuredTask.getSkipHints().stream()
                .map(Supplier::get)
                .collect(Collectors.toList());
        } catch (final RuntimeException e) {
            throw new IllegalStateException("Error while evaluating skip hints for task "
                + configuredTask, e);
        }

        // Prevent skipping tasks by default
        if (skipHints.isEmpty() && !configuredTask.isGoal()) {
            LOG.debug("No skip hints configured -- don't skip");
            return "PREVENT-SKIP:" + SkipChecksumUtil.never();
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

            try {
                final String checksum = usedProducts
                    .readProduct(moduleName, productId, ManagedProduct.class)
                    .map(ManagedProduct::checksum)
                    .orElse("NO_PRODUCT");

                checksumParts.add(String.format("%s#%s:%s", moduleName, productId, checksum));
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }

        final String hash = Hashing.hash(checksumParts);

        LOG.debug("Signature: {}; Hash: {}", checksumParts, hash);

        return hash;
    }

    boolean canSkipTask() {
        if (Files.notExists(sigFile)) {
            return false;
        }

        try {
            final String signatureLastRun = FileUtil.readToString(sigFile);

            final boolean equals = signatureLastRun.equals(signature);
            LOG.info("Last run: {} - current: {}; equals: {}", signatureLastRun, signature, equals);
            return equals;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    private Path buildFileName() {
        final Path file = LoomPaths.loomDir(runtimeConfiguration.getProjectBaseDir())
            .resolve(Paths.get(LoomVersion.getVersion(), "execution-prevention", "checksums",
                configuredTask.getBuildContext().getModuleName(),
                configuredTask.getProvidedProduct() + ".sig"));

        try {
            Files.createDirectories(file.getParent());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return file;
    }

    void clearSignature() {
        try {
            Files.deleteIfExists(sigFile);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void commitSignature() {
        try {
            FileUtil.writeStringToFile(sigFile, signature, StandardOpenOption.CREATE_NEW);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
