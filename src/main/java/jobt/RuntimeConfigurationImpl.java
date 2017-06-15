package jobt;

import jobt.api.RuntimeConfiguration;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {

    private final boolean cacheEnabled;

    public RuntimeConfigurationImpl(final boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

}
