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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GenericProduct implements Product {

    private final Map<String, Object> properties;
    private final String checksum;
    private final String outputInfo;

    public GenericProduct(final Map<String, Object> properties, final String checksum,
                          final String outputInfo) {
        Objects.requireNonNull(properties, "properties required");
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        this.checksum = Objects.requireNonNull(checksum, "checksum required");
        this.outputInfo = outputInfo;
    }

    @Override
    public <T> T getProperty(final Class<T> clazz, final String key) {
        return clazz.cast(properties.get(key));
    }

    @Override
    public <T> List<T> getProperties(final Class<T> clazz, final String key) {
        return (List<T>) properties.get(key);
    }

    @Override
    public String checksum() {
        return checksum;
    }

    @Override
    public Optional<String> outputInfo() {
        return Optional.ofNullable(outputInfo);
    }

    @Override
    public String toString() {
        return "GenericProduct{"
            + "properties=" + properties
            + ", checksum='" + checksum + '\''
            + ", outputInfo='" + outputInfo + '\''
            + '}';
    }

}
