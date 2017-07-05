package builders.loom.api;

import builders.loom.api.service.ServiceLocator;

public interface ServiceLocatorAware {

    void setServiceLocator(ServiceLocator serviceLocator);

}
