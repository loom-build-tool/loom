package jobt.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import jobt.api.ExecutionContext;
import jobt.util.UncheckedExecutionException;

public class ExecutionContextImpl implements ExecutionContext {

    // compile/test scope dependencies
    final CompletableFuture<List<Path>> compileDependenciesPromise = new CompletableFuture<>();
    final CompletableFuture<List<Path>> testDependenciesPromise = new CompletableFuture<>();
    private final CountDownLatch compileClassPathLatch = new CountDownLatch(1);
    private final CountDownLatch testClassPathLatch = new CountDownLatch(1);

    private volatile List<URL> compileClasspath;
    private volatile List<URL> testClasspath;

    /**
     * Resolved by mavenresolver plugin
     */
    @Override
    public CompletableFuture<List<Path>> getCompileDependenciesPromise() {
        return compileDependenciesPromise;
    }

    @Override
    public List<Path> getResolvedCompileDependencies() throws InterruptedException {
        try {
            return compileDependenciesPromise.get();
        } catch (final ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    /**
     * Resolved by mavenresolver plugin
     */
    @Override
    public CompletableFuture<List<Path>> getTestDependenciesPromise() {
        return testDependenciesPromise;
    }

    @Override
    public List<Path> getResolvedTestDependencies() throws InterruptedException {
        try {
            return testDependenciesPromise.get();
        } catch (final ExecutionException e) {
            throw new UncheckedExecutionException(e);
        }
    }

    @Override
    public List<URL> getCompileClasspath() throws InterruptedException {
        compileClassPathLatch.await();
        return compileClasspath;
    }

    @Override
    public void setCompileClasspath(final List<URL> compileClasspath) {
        this.compileClasspath = compileClasspath;
        compileClassPathLatch.countDown();
    }

    @Override
    public List<URL> getTestClasspath() throws InterruptedException {
        testClassPathLatch.await();
        return testClasspath;
    }

    @Override
    public void setTestClasspath(final List<URL> testClasspath) {
        this.testClasspath = testClasspath;
        testClassPathLatch.countDown();
    }

}
