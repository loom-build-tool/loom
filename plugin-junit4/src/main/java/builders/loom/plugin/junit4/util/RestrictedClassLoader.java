package builders.loom.plugin.junit4.util;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestrictedClassLoader extends ClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger(RestrictedClassLoader.class);

    // please add DOTs
    private static final List<String> WHITELISTED_PACKAGES =
        Arrays.asList("builders.loom.plugin.junit4.shared.");

    public RestrictedClassLoader(final ClassLoader parent) {
        super(parent);
    }


    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
        if (!classAllowed(name)) {
            throw new IllegalStateException("Access to class not allowed: " + name);
        }
        return super.loadClass(name, resolve);
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
