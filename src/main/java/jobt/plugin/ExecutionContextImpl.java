package jobt.plugin;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ExecutionContextImpl implements ExecutionContext {

    private CountDownLatch compileClassPathLatch = new CountDownLatch(1);
    private CountDownLatch testClassPathLatch = new CountDownLatch(1);

    private volatile List<URL> compileClasspath;
    private volatile List<URL> testClasspath;

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
        this.testClasspath = compileClasspath;
        testClassPathLatch.countDown();
    }

}
