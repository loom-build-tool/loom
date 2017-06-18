package jobt.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ExecutionContext;
import jobt.api.ProductPromise;

public class ExecutionContextImpl implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionContextImpl.class);

    private static final int FUTURE_WAIT_THRESHOLD = 30;

    // FIXME
    private final Map<String, ProductPromise> products = new HashMap<>();

    @Override
    public Map<String, ProductPromise> getProducts() {
        return products;
    }

    // compile/test scope dependencies
    private final CompletableFuture<List<Path>> compileDependencies =
        new CompletableFuture<>();
    private final CompletableFuture<List<Path>> testDependencies = new CompletableFuture<>();

    private final CompletableFuture<List<URL>> compileClasspath = new CompletableFuture<>();
    private final CompletableFuture<List<URL>> testClasspath = new CompletableFuture<>();

    @Override
    public void setCompileClasspath(final List<URL> compileClasspath) {
        complete(this.compileClasspath, compileClasspath);
    }

    @Override
    public List<URL> getCompileClasspath() {
        return waitAndGet(compileClasspath);
    }

    @Override
    public void setTestClasspath(final List<URL> testClasspath) {
        complete(this.testClasspath, testClasspath);
    }

    @Override
    public List<URL> getTestClasspath() {
        return waitAndGet(testClasspath);
    }

//    @Override
//    public void setCompileDependencies(final List<Path> compileDependencies) {
//        complete(this.compileDependencies, compileDependencies);
//    }
//
//    @Override
//    public List<Path> getCompileDependencies() {
//        return waitAndGet(compileDependencies);
//    }

//    @Override
//    public void setTestDependencies(final List<Path> testDependencies) {
//        complete(this.testDependencies, testDependencies);
//    }
//
//    @Override
//    public List<Path> getTestDependencies() {
//        return waitAndGet(testDependencies);
//    }

    private <T> void complete(final CompletableFuture<T> promise, final T withValue) {
        final boolean completed = promise.complete(withValue);
        if (!completed) {
            throw new IllegalStateException("Future already completed!");
        }
    }

    private static <T> T waitAndGet(final Future<T> future) {
        try {
            return future.get(FUTURE_WAIT_THRESHOLD, TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        } catch (final TimeoutException e) {
            try {
                LOG.warn("Already waiting for {} seconds - continue", FUTURE_WAIT_THRESHOLD);
                return future.get();
            } catch (final ExecutionException e1) {
                throw new IllegalStateException(e1);
            } catch (final InterruptedException e1) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e1);
            }
        }
    }

}
