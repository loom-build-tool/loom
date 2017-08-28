package builders.loom.plugin.junit4.wrapper;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class MavenRunnerTest {

	@Test
	public void test() {

		 final List<Class<?>> testClasses = Arrays.asList(sample.SimpleFailTest.class);
		 
		 new JUnit4Wrapper().mavenTestRun(testClasses);
		 
	}

}
