package builders.loom.api.service;

import java.util.Set;

import builders.loom.api.Service;

public interface ServiceLocator {

    <T extends Service> T getService(String name, Class<T> clazz);

    Set<String> getServiceNames();

}
