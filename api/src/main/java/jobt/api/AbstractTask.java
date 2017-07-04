package jobt.api;

import jobt.api.service.ServiceLocator;

public abstract class AbstractTask implements Task,
    ProductDependenciesAware, ServiceLocatorAware {

    private ProvidedProducts providedProducts;
    private UsedProducts usedProducts;
    private ServiceLocator serviceLocator;

    @Override
    public void setProvidedProducts(final ProvidedProducts providedProducts) {
        this.providedProducts = providedProducts;
    }

    public ProvidedProducts getProvidedProducts() {
        return providedProducts;
    }

    @Override
    public void setUsedProducts(final UsedProducts usedProducts) {
        this.usedProducts = usedProducts;
    }

    public UsedProducts getUsedProducts() {
        return usedProducts;
    }

    @Override
    public void setServiceLocator(final ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

}
