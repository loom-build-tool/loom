package jobt.api.service;

import java.util.function.Supplier;

import jobt.api.Service;

public interface ServiceLocatorRegistration {

    void registerService(String serviceName, Supplier<Service> serviceSupplier);

}
