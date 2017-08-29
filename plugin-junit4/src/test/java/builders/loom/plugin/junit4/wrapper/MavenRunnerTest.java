package builders.loom.plugin.junit4.wrapper;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class MavenRunnerTest {

	@Test
	public void test1() {
		
	        final RunResult result = new JUnit4Wrapper().mavenTestRun(Arrays.asList(sample.SimplePassTest.class));
	        
	        assertResult(2, 0, 0, 0, result);

	}

	@Test
	public void test2() {

		 final RunResult result = new JUnit4Wrapper().mavenTestRun(Arrays.asList(sample.SimpleFailTest.class));
		 
		 assertResult(1, 1, 0, 0, result);

	}
	
	@Test
	public void testSuite() {
		
		final RunResult result = new JUnit4Wrapper().mavenTestRun(Arrays.asList(sample.UnsupportedTestSuite.class));
		 
		 assertResult(2, 1, 0, 0, result);
		 
	}
	
	@Test
	public void testSkipped() {
		
		final RunResult result = new JUnit4Wrapper().mavenTestRun(Arrays.asList(sample.SkippedTest.class));
		 
		 assertResult(2, 0, 1, 0, result);
		 
	}
	
	@Test
	public void testBrokenSetup() {
		
		final RunResult result = new JUnit4Wrapper().mavenTestRun(Arrays.asList(sample.SetupFailedTest.class));
		 
		 assertResult(1, 0, 0, 1, result);
		 
	}
	

	private void assertResult(
	        final int completedCount,
	        final int failureCount,
	        final int skippedCount,
	        final int errorCount,
	        final RunResult result) {

		assertEquals("completed", completedCount, result.getCompletedCount());
		assertEquals("failures", failureCount, result.getFailures());
		assertEquals("skipped", skippedCount, result.getSkipped());
		assertEquals("errors", errorCount, result.getErrors());
		
	}

}
