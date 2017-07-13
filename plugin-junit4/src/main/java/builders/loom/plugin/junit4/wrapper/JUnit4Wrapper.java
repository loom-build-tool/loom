package builders.loom.plugin.junit4.wrapper;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.plugin.junit4.shared.TestResult;

/**
 * Wrapper gets injected into final classloader.
 */
public class JUnit4Wrapper {

    private static final Logger LOG = LoggerFactory.getLogger(JUnit4Wrapper.class);

    public TestResult run(final ClassLoader classLoader, final Class[] testClasses) {

        Thread.currentThread().setContextClassLoader(classLoader);

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        final Result run = jUnitCore.run(computer, testClasses);

        jUnitCore.addListener(new RunListener() {
        });

        run.getFailures().forEach(f ->
            LOG.info(" Test failure: " + f));

        return new TestResult(run.wasSuccessful());

    }

}