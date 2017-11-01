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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ManagedGenericProduct extends UnmanagedGenericProduct implements ManagedProduct {

    private final String checksum;

    public ManagedGenericProduct(final String key, final String value, final String checksum,
                                 final OutputInfo outputInfo) {
        this(Map.of(key, List.of(value)), checksum, outputInfo);
    }

    public ManagedGenericProduct(final Map<String, List<String>> properties, final String checksum,
                                 final OutputInfo outputInfo) {
        super(properties, outputInfo);
        this.checksum = Objects.requireNonNull(checksum, "checksum required");
    }

    @Override
    public String checksum() {
        return checksum;
    }

    @Override
    public String toString() {
        return "ManagedGenericProduct{"
            + "checksum='" + checksum + '\''
            + "} " + super.toString();
    }

}
