package builders.loom.util;

import java.util.Collection;

public final class Iterables {

    private Iterables() {
    }

    public static <T> T getOnlyElement(final Collection<T> collection) {
        if (collection.size() == 1) {
            return collection.iterator().next();
        }

        throw new IllegalArgumentException("Expected one element, but got " + collection.size());
    }

    public static <T> T getOnlyElement(final Collection<T> collection, final T defaultValue) {
        return !collection.isEmpty() ? getOnlyElement(collection) : defaultValue;
    }

}
