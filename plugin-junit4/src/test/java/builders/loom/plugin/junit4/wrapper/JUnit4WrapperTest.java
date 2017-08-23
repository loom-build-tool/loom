package builders.loom.plugin.junit4.wrapper;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class JUnit4WrapperTest {

	@Test
	public void test1() {
		
		  final Computer computer = new Computer();
	        final JUnitCore jUnitCore = new JUnitCore();
	        jUnitCore.addListener(new RunListener() {
	            @Override
	            public void testFailure(final Failure failure) throws Exception {
	            		System.out.println(failure.toString());
	            }

	        });

	        final List<Class<?>> testClasses = List.of(sample.SimplePassTest.class);
	        
	        final Result run = jUnitCore.run(computer, testClasses.toArray(new Class[]{}));
	        
	        assertResult(true, 2, 0, 0, run);

	}

	@Test
	public void test2() {
		
		  final Computer computer = new Computer();
	        final JUnitCore jUnitCore = new JUnitCore();
	        jUnitCore.addListener(new RunListener() {
	            @Override
	            public void testFailure(final Failure failure) throws Exception {
	            		System.out.println(failure.toString());
	            }

	        });

	        final List<Class<?>> testClasses = List.of(sample.SimpleFailTest.class);
	        
	        final Result run = jUnitCore.run(computer, testClasses.toArray(new Class[]{}));
	        
	        assertResult(false, 1, 1, 0, run);

	}
	
	@Test
	public void testSuite() {
		
		  final Computer computer = new Computer();
	        final JUnitCore jUnitCore = new JUnitCore();
	        jUnitCore.addListener(new RunListener() {
	            @Override
	            public void testFailure(final Failure failure) throws Exception {
	            		System.out.println(failure.toString());
	            }

	        });

	        final List<Class<?>> testClasses = List.of(sample.UnsupportedTestSuite.class);
	        
	        final Result run = jUnitCore.run(computer, testClasses.toArray(new Class[]{}));
	        
	        assertResult(false, 2, 1, 0, run);

	}
	
	private void assertResult(
	        final boolean successful,
	        final int runCount,
	        final int failureCount,
	        final int ignoreCount,
	        final Result result) {

		assertEquals("successful?", successful, result.wasSuccessful());
		assertEquals("runCount", runCount, result.getRunCount());
		assertEquals("failureCount", failureCount, result.getFailureCount());
		assertEquals("ignoreCount", ignoreCount, result.getIgnoreCount());
		
	}
	
}
