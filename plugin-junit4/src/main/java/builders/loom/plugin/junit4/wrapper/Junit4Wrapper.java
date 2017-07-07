package builders.loom.plugin.junit4.wrapper;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import builders.loom.plugin.junit4.TestResult;

public class Junit4Wrapper {

    public TestResult run(final ClassLoader classLoader, final Class[] testClasses) {

        Thread.currentThread().setContextClassLoader(classLoader);

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        final Result run = jUnitCore.run(computer, testClasses);

        jUnitCore.addListener(new RunListener() {
        });

        return new TestResult(run.wasSuccessful());

    }

}