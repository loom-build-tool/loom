package jobt.plugin.junit4;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Predicate;

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
        if (!injectPredicate.test(name)) {
            return super.loadClass(name);
        }
        try(InputStream in =
            extraClassesLoader.getResourceAsStream(Junit4TestTask.classRes(name))) {
            Objects.requireNonNull(in, "Failed to load class for injection: " + name);
            final byte[] a = new byte[10000]; // FIXME
            final int len  = in.read(a);
            return defineClass(name, a, 0, len);
        } catch (final IOException e) {
            throw new ClassNotFoundException();
        }
    }
}