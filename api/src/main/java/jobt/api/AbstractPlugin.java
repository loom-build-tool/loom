package jobt.api;

import java.util.Arrays;
import java.util.HashSet;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin implements Plugin {

    private BuildConfig buildConfig;
    private RuntimeConfiguration runtimeConfiguration;
    private ProductRepository productRepository;

    @Override
    public void setBuildConfig(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    public BuildConfig getBuildConfig() {
        return buildConfig;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    @Override
    public void setProductRepository(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public final ProvidedProducts provides(final String... productIdLists) {
        return new ProvidedProducts(
            new HashSet<>(Arrays.asList(productIdLists)), productRepository);
    }

}
