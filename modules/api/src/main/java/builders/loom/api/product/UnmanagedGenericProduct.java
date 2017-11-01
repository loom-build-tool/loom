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

package builders.loom.api.product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class UnmanagedGenericProduct implements Product {

    private final Map<String, List<String>> properties;
    private final OutputInfo outputInfo;

    public UnmanagedGenericProduct(final String key, final String value,
                                   final OutputInfo outputInfo) {
        this(Map.of(key, List.of(value)), outputInfo);
    }

    public UnmanagedGenericProduct(final Map<String, List<String>> properties,
                                   final OutputInfo outputInfo) {
        Objects.requireNonNull(properties, "properties required");
        if (properties.values().stream().anyMatch(v -> v == null || v.isEmpty())) {
            throw new IllegalArgumentException("properties must not contain null/empty values");
        }
        this.properties = immutable(properties);
        this.outputInfo = outputInfo;
    }

    private static Map<String, List<String>> immutable(final Map<String, List<String>> properties) {
        final Map<String, List<String>> propCopy = properties.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> Collections.unmodifiableList(new ArrayList<>(e.getValue()))));

        return Collections.unmodifiableMap(propCopy);
    }

    @Override
    public String getProperty(final String key) {
        return getProperties(key).iterator().next();
    }

    @Override
    public List<String> getProperties(final String key) {
        final List<String> val = properties.get(key);
        if (val == null) {
            throw new IllegalArgumentException("key <" + key + "> not found in properties");
        }
        return val;
    }

    @Override
    public Map<String, List<String>> getProperties() {
        return properties;
    }

    @Override
    public Optional<OutputInfo> getOutputInfo() {
        return Optional.ofNullable(outputInfo);
    }

    @Override
    public String toString() {
        return "UnmanagedGenericProduct{"
            + "properties=" + properties
            + ", outputInfo='" + outputInfo + '\''
            + '}';
    }

}
