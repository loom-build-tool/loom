package jobt.plugin.java;

public interface KeyValueCache {
    void saveCache();

    Long get(String key);

    void put(String key, Long value);

    void remove(String key);
}
