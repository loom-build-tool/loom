package jobt.api.service;

import java.util.Set;
import java.util.function.Supplier;

import jobt.api.Service;

public interface ServiceLocator {

    void registerService(String serviceName, Supplier<Service> serviceSupplier);

    <T extends Service> T getService(String name, Class<T> clazz);

    Set<String> getServiceNames();

}
