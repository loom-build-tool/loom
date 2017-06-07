package jobt.plugin.execution;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;


public final class Dispatcher {

    private Dispatcher() {
    }

    public static <T extends ExecArgs>  void executeInWorkerClassLoader(
        final ClassLoader executionScopeClassLoader,
        final Class<? extends WorkerProtocol<T>> workerImplementationClass, final T spec) throws Exception {

        final ClassLoader backup = Thread.currentThread().getContextClassLoader();
        try {
            // TODO do we need this ?
            Thread.currentThread().setContextClassLoader(executionScopeClassLoader);
            final Callable<?> worker = transferWorkerIntoWorkerClassloader(
                workerImplementationClass, spec, executionScopeClassLoader);
            final Object result = worker.call();
        } catch (final Exception e) {
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(backup);
        }
    }

    public static <T extends ExecArgs> Callable<?> transferWorkerIntoWorkerClassloader(
        final Class<? extends WorkerProtocol<T>> workerImplementationClass, final T spec, final ClassLoader workerClassLoader) {
        final byte[] serializedWorker = serialize(new WorkerCallable<>(workerImplementationClass, spec));
        try (ObjectInputStream ois =
            new ClassLoaderObjectInputStream(
                new ByteArrayInputStream(serializedWorker), workerClassLoader)) {

            return (Callable<?>) ois.readObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] serialize(final Object object) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try(final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(object);
            objectOutputStream.close();
            return outputStream.toByteArray();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }}

    public static interface ExecArgs extends Serializable {

        public static ExecArgs NOOP = new ExecArgs() {
            private static final long serialVersionUID = 1L;
        };

    }

    public interface WorkerProtocol<T extends ExecArgs> {
        DefaultWorkResult execute(T spec);
    }


}
