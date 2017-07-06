package jobt.plugin.junit4.wrapper;

import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class Junit4Wrapper {

    public boolean run(final Class[] testClasses) {

//        Thread.currentThread().setContextClassLoader(classLoader);

        System.out.println("run test on " + testClasses);
        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        final Result run = jUnitCore.run(computer, testClasses);
        jUnitCore.addListener(new RunListener() {
            @Override
            public void testRunStarted(final Description description) throws Exception {
                System.out.println("Test started: " + description.getMethodName());
            }
            @Override
            public void testIgnored(final Description description) throws Exception {
                System.out.println("Test ignored: " + description.getMethodName());
            }
            @Override
            public void testStarted(final Description description) throws Exception {
                System.out.println("Test run: " + description.getMethodName());
            }
            @Override
            public void testFailure(final Failure failure) throws Exception {
                System.err.println("Test failure: " + failure);
            }
        });
        System.out.println("success="+run.wasSuccessful());
        System.out.println("result="+run.getFailures());

        return run.wasSuccessful();

    }

    public void shoot() {
        System.out.println("shoot");
    }
}