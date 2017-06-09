package jobt.plugin.java;

/**
 * Created by oliver on 09.06.17.
 */
public interface KeyValueCache {
    void saveCache();

    Long get(String key);

    void put(String key, Long value);

    void remove(String key);
}
