package jobt.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ExecutionContext;

public class ExecutionContextImpl implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionContextImpl.class);

    private static final int FUTURE_WAIT_THRESHOLD = 30;

    // compile/test scope dependencies
    private final CompletableFuture<List<Path>> compileDependenciesPromise =
        new CompletableFuture<>();
    private final CompletableFuture<List<Path>> testDependenciesPromise = new CompletableFuture<>();

    private final CompletableFuture<List<URL>> compileClasspath = new CompletableFuture<>();
    private final CompletableFuture<List<URL>> testClasspath = new CompletableFuture<>();

    /**
     * Provided by mavenresolver plugin.
     */
    @Override
    public CompletableFuture<List<Path>> getCompileDependenciesPromise() {
        return compileDependenciesPromise;
    }

    @Override
    public List<Path> getResolvedCompileDependencies() {
        return waitAndGet(compileDependenciesPromise);
    }

    /**
     * Provided by mavenresolver plugin.
     */
    @Override
    public CompletableFuture<List<Path>> getTestDependenciesPromise() {
        return testDependenciesPromise;
    }

    @Override
    public List<Path> getResolvedTestDependencies() {
        return waitAndGet(testDependenciesPromise);
    }

    @Override
    public List<URL> getCompileClasspath() throws InterruptedException {
        return waitAndGet(compileClasspath);
    }

    @Override
    public void setCompileClasspath(final List<URL> compileClasspath) {
        this.compileClasspath.complete(compileClasspath);
    }

    @Override
    public List<URL> getTestClasspath() throws InterruptedException {
        return waitAndGet(testClasspath);
    }

    @Override
    public void setTestClasspath(final List<URL> testClasspath) {
        this.testClasspath.complete(testClasspath);
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
