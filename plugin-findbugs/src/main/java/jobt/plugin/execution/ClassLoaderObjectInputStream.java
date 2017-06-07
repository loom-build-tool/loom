package jobt.plugin.execution;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public final class ClassLoaderObjectInputStream extends ObjectInputStream {

    private final ClassLoader classLoader;

    public ClassLoaderObjectInputStream(final InputStream in, final ClassLoader loader) throws IOException {
        super(in);
        this.classLoader = loader;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass cd) throws IOException, ClassNotFoundException {
        try {

            return Class.forName(cd.getName(), false, classLoader);

        } catch (final ClassNotFoundException e) {

            return super.resolveClass(cd);

        }
    }
}