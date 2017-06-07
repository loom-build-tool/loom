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
    private static final List<String> WHITELISTED_PACKAGES = Arrays.asList("org.slf4j.", "jobt.api.");

    private final ClassLoader appClassLoader;

    public BiSectFilteringClassLoader(final ClassLoader extClassLoader, final ClassLoader appClassLoader) {
        super(extClassLoader);
        this.appClassLoader = appClassLoader;
    }


    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        try {
            final Class<?> cl = super.loadClass(name, false);
            if (resolve) {
                resolveClass(cl);
            }
            return cl;
        } catch (final ClassNotFoundException ignore) {
            // ignore
        }

        if (!classAllowed(name)) {
            throw new ClassNotFoundException(name + " not found.");
        }

        return appClassLoader.loadClass(name);
    }

    private boolean classAllowed(final String className) {

        final boolean allowed = isWhitelistedPackage(className);

        if (LOG.isTraceEnabled()) {
            LOG.trace("class <"+className+"> is "+(allowed?"allowed":"not allowed"));
        }
        return allowed;
    }

    private boolean isWhitelistedPackage(final String className) {
        return WHITELISTED_PACKAGES.stream().anyMatch(className::startsWith);
    }

}
