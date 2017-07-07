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

package builders.loom.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import builders.loom.api.Service;
import builders.loom.api.service.ServiceLocator;
import builders.loom.api.service.ServiceLocatorRegistration;

public class ServiceLocatorImpl implements ServiceLocator, ServiceLocatorRegistration {

    private final Map<String, Supplier<Service>> registry = new ConcurrentHashMap<>();

    @Override
    public void registerService(final String serviceName, final Supplier<Service> serviceSupplier) {
        Objects.requireNonNull(serviceName, "serviceName must be specified");
        Objects.requireNonNull(serviceSupplier,
            "serviceSupplier missing on service <" + serviceName + ">");

        if (registry.putIfAbsent(serviceName, serviceSupplier) != null) {
            throw new IllegalStateException("Service with name " + serviceName
                + " already registered.");
        }
    }

    @Override
    public <T extends Service> T getService(final String serviceName, final Class<T> serviceClazz) {
        Objects.requireNonNull(serviceName, "serviceName must be specified");
        Objects.requireNonNull(serviceClazz, "serviceClazz must be specified");

        final Supplier<Service> supplier = registry.get(serviceName);
        Objects.requireNonNull(supplier, "No service supplier found with name: " + serviceName);
        final Service service = supplier.get();
        Objects.requireNonNull(supplier,
            "No service returned from supplier using name: " + serviceName);
        return serviceClazz.cast(service);
    }

    @Override
    public Set<String> getServiceNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

}
