package builders.loom.api.service;

import java.util.function.Supplier;

import builders.loom.api.Service;

public interface ServiceLocatorRegistration {

    void registerService(String serviceName, Supplier<Service> serviceSupplier);

}
