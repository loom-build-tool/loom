package jobt.api;

import jobt.api.service.ServiceLocator;

public interface ServiceLocatorAware {

    void setServiceLocator(ServiceLocator serviceLocator);

}
