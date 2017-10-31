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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import builders.loom.api.LoomPaths;
import builders.loom.api.RuntimeConfiguration;
import builders.loom.api.TaskResult;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.OutputInfo;
import builders.loom.api.product.Product;
import builders.loom.core.plugin.ConfiguredTask;
import builders.loom.util.FileUtil;
import builders.loom.util.serialize.Record;
import builders.loom.util.serialize.SimpleSerializer;

class CachedProduct {

    private final RuntimeConfiguration runtimeConfiguration;
    private final ConfiguredTask configuredTask;

    CachedProduct(final RuntimeConfiguration runtimeConfiguration,
                  final ConfiguredTask configuredTask) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.configuredTask = configuredTask;
    }

    private Path buildFileName(final String suffix) {
        return LoomPaths.loomDir(runtimeConfiguration.getProjectBaseDir())
            .resolve(Paths.get(Version.getVersion(), "execution-prevention", "products",
                configuredTask.getBuildContext().getModuleName(),
                configuredTask.getProvidedProduct() + suffix));
    }

    boolean available() {
        return Files.exists(buildFileName(".product"));
    }

    Product load() {
        final Path productFile = buildFileName(".product");
        final Path productInfoFile = buildFileName(".product.info");
        final Path checksumFile = buildFileName(".product.checksum");

        final Map<String, List<String>> properties = new HashMap<>();

        try {
            final String checksum = FileUtil.readToString(checksumFile);

            if ("EMPTY".equals(checksum)) {
                return null;
            }

            SimpleSerializer.read(productFile, (e) -> {
                final String key = e.getFields().get(0);
                final List<String> values = e.getFields().subList(1, e.getFields().size());
                properties.put(key, values);
            });

            final AtomicReference<OutputInfo> outputInfo = new AtomicReference<>();

            if (Files.exists(productInfoFile)) {
                SimpleSerializer.read(productInfoFile, (e) -> {
                    final String name = e.getFields().get(0);
                    final Path artifact = Paths.get(e.getFields().get(1));
                    outputInfo.set(new OutputInfo(name, artifact));
                });
            }

            return new GenericProduct(properties, checksum, outputInfo.get());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void prepare() {
        try {
            Files.deleteIfExists(buildFileName(".product.checksum"));
            Files.deleteIfExists(buildFileName(".product.info"));
            Files.deleteIfExists(buildFileName(".product"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void persist(final TaskResult taskResult) {
        final Path productFile = buildFileName(".product");
        final Path productInfoFile = buildFileName(".product.info");
        final Path checksumFile = buildFileName(".product.checksum");

        try {
            Files.createDirectories(checksumFile.getParent());

            final String checksum;
            if (taskResult.getStatus() == TaskStatus.EMPTY) {
                checksum = "EMPTY";
            } else {
                final Product product = taskResult.getProduct()
                    .orElseThrow(() -> new IllegalStateException("No Product available"));

                final Map<String, List<String>> properties = product.getProperties();

                SimpleSerializer.write(productFile, properties.entrySet(), (e) -> {
                    final List<String> elements = new ArrayList<>();
                    elements.add(e.getKey());
                    elements.addAll(e.getValue());
                    return new Record(elements);
                });

                if (product.getOutputInfo().isPresent()) {
                    final OutputInfo outputInfo = product.getOutputInfo().get();

                    final Path relativeArtifactPath = runtimeConfiguration.getProjectBaseDir()
                        .toAbsolutePath().relativize(outputInfo.getArtifact().toAbsolutePath());

                    SimpleSerializer.write(productInfoFile, List.of(outputInfo),
                        (e) -> new Record(e.getName(), relativeArtifactPath.toString()));
                }

                checksum = product.checksum();
            }

            FileUtil.writeStringToFile(checksumFile, checksum, StandardOpenOption.CREATE_NEW);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
