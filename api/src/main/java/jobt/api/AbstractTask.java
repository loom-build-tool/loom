package jobt.api;

public abstract class AbstractTask implements Task {

    private ProvidedProducts providedProducts;
    private UsedProducts usedProducts;

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

}
