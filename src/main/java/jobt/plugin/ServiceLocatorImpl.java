package jobt.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jobt.api.Service;
import jobt.api.service.ServiceLocator;
import jobt.api.service.ServiceLocatorRegistration;

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
