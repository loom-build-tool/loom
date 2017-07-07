package builders.loom.plugin.junit4;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Predicate;

import jobt.util.Util;

/**
 * Wrap parent classloader enriching it with classes loader from extraClassesLoader.
 *
 */
public class InjectingClassLoader extends ClassLoader {

    private final ClassLoader extraClassesLoader;
    private final Predicate<String> injectPredicate;

    public InjectingClassLoader(
        final ClassLoader parent, final ClassLoader extraClassesLoader,
        final Predicate<String> injectPredicate) {
        super(parent);
        this.injectPredicate = injectPredicate;
        Objects.requireNonNull(parent);
        Objects.requireNonNull(extraClassesLoader);
        this.extraClassesLoader = extraClassesLoader;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        System.out.println(" --INJECT=-> " + name);
        if (!injectPredicate.test(name)) {
            return super.loadClass(name);
        }
        try(InputStream in =
            extraClassesLoader.getResourceAsStream(Util.resourceNameFromClassName(name))) {
            Objects.requireNonNull(in, "Failed to load class for injection: " + name);
            final byte[] classBytes = Util.toByteArray(in);
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (final IOException e) {
            throw new ClassNotFoundException();
        }
    }
}