package builders.loom.plugin.junit4.wrapper;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class Junit4Wrapper {

    public boolean run(final ClassLoader classLoader, final Class[] testClasses) {

        Thread.currentThread().setContextClassLoader(classLoader);

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
        final Result run = jUnitCore.run(computer, testClasses);

        jUnitCore.addListener(new RunListener() {
        });

        run.getFailures().forEach(f ->
        System.out.println(" -F-> " + f));

        return run.wasSuccessful();
//        return new TestResult(run.wasSuccessful());

    }

}