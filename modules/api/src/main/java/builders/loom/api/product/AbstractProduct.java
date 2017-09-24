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
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractProduct implements Product {

    private final String checksum = UUID.randomUUID().toString();

    @Override
    public <T> T getProperty(final Class<T> clazz, final String key) {
        return null;
    }

    @Override
    public <T> List<T> getProperties(final Class<T> clazz, final String key) {
        return null;
    }

    @Override
    public Optional<String> outputInfo() {
        return Optional.empty();
    }

    @Override
    public String checksum() {
        return checksum;
    }

}
