package builders.loom.plugin.java;

public final class NullKeyValueCache implements KeyValueCache {

    @Override
    public void saveCache() {
    }

    @Override
    public Long get(final String key) {
        return null;
    }

    @Override
    public void put(final String key, final Long value) {
    }

    @Override
    public void remove(final String key) {
    }

}
