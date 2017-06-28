package jobt.plugin;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load classes from extClassLoader and additionally from appClassLoader if whitelisted.
 */
public final class BiSectFilteringClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(BiSectFilteringClassLoader.class);

    // please add DOTs
    private static final List<String> WHITELISTED_PACKAGES =
        Arrays.asList("org.slf4j.", "jobt.api.", "jobt.util.");

    private final ClassLoader appClassLoader;

    public BiSectFilteringClassLoader(
        final ClassLoader extClassLoader, final ClassLoader appClassLoader) {
        super(extClassLoader);
        this.appClassLoader = appClassLoader;
        checkHierarchy(extClassLoader, appClassLoader);
    }

    private static void checkHierarchy(
        final ClassLoader extClassLoader, final ClassLoader appClassLoader) {

        boolean isOk = false;

        for (ClassLoader cl = appClassLoader; cl != null; cl = cl.getParent()) {
            if (cl.equals(extClassLoader)) {
                isOk = true;
            }
        }

        if (!isOk) {
            throw new IllegalArgumentException("ClassLoaders must be in one hierarchy");
        }

    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (final ClassNotFoundException notFound) {
            if (classAllowed(name)) {
                return appClassLoader.loadClass(name);
            } else {
                throw notFound;
            }
        }
    }

    private boolean classAllowed(final String className) {

        final boolean allowed = isWhitelistedPackage(className);

        LOG.trace("Class <{}> is {}", className, allowed ? "whitelisted" : "not whitelisted");

        return allowed;
    }

    private boolean isWhitelistedPackage(final String className) {
        return WHITELISTED_PACKAGES.stream().anyMatch(className::startsWith);
    }

}
