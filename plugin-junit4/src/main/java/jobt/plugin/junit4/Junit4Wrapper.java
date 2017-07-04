package jobt.plugin.junit4;

import java.util.Arrays;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

public class Junit4Wrapper {

    static {
        System.err.println("LOADED");
    }

    public boolean run(Class[] testClasses) {

        System.out.println("Run testClasses: " + Arrays.toString(testClasses));

        final Computer computer = new Computer();
        final JUnitCore jUnitCore = new JUnitCore();
//        jUnitCore.addListener(new RunListener() {
//            @Override
//            public void testFailure(final Failure failure) throws Exception {
//                System.err.println("Test failure: " + failure);
//            }
//        });
        final Result run = jUnitCore.run(computer, testClasses);
        return run.wasSuccessful();

    }

}
