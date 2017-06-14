package jobt.plugin;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jobt.api.ExecutionContext;

public class ExecutionContextImpl implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionContextImpl.class);

    private final CountDownLatch compileClassPathLatch = new CountDownLatch(1);
    private final CountDownLatch testClassPathLatch = new CountDownLatch(1);

    private AtomicReference<List<URL>> compileClasspath = new AtomicReference<>();
    private AtomicReference<List<URL>> testClasspath = new AtomicReference<>();

    @Override
    public List<URL> getCompileClasspath() throws InterruptedException {
        LOG.info("Wait for latch to getCompileClasspath");
        compileClassPathLatch.await();
        LOG.info("getCompileClasspath");
        return compileClasspath.get();
    }

    @Override
    public void setCompileClasspath(final List<URL> compileClasspath) {
        LOG.info("setCompileClasspath");
        this.compileClasspath.set(compileClasspath);
        compileClassPathLatch.countDown();
    }

    @Override
    public List<URL> getTestClasspath() throws InterruptedException {
        LOG.info("Wait for latch to getTestClasspath");
        testClassPathLatch.await();
        LOG.info("getTestClasspath");
        return testClasspath.get();
    }

    @Override
    public void setTestClasspath(final List<URL> testClasspath) {
        LOG.info("setTestClasspath");
        this.testClasspath.set(testClasspath);
        testClassPathLatch.countDown();
    }

}
