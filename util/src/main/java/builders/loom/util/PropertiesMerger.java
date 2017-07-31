package builders.loom.util;

import java.util.Objects;
import java.util.Properties;

public class PropertiesMerger {

    private final Properties wrapper;
    private boolean changed;

    public PropertiesMerger(Properties properties) {
        Objects.requireNonNull(properties);
        this.wrapper = properties;
    }

    /**
     * Replace value with new value only if it exists.
     */
    public void fixup(String key, String newValue) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(newValue);
        if (wrapper.containsKey(key)) {
            final Object prevValue = wrapper.setProperty(key, newValue);
            changed |= !newValue.equals(prevValue);
        }
    }

    /**
     * Set value if not defined.
     */
    public void setIfAbsent(String key, String value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        if (!wrapper.containsKey(key)) {
            wrapper.setProperty(key, value);
            changed = true;
        }
    }

    /**
     * Report, if values have changed through merger wrapper.
     * Note: direct changes made to the wrapped object are not tracked!
     */
    public boolean isChanged() {
        return changed;
    }
}
