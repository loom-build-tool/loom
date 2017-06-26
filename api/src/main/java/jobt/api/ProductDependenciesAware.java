package jobt.api;


public interface ProductDependenciesAware {

    void setProvidedProducts(ProvidedProducts providedProducts);

    void setUsedProducts(UsedProducts usedProducts);

}
