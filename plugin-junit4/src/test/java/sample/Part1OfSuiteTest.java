package sample;

import org.junit.Assert;
import org.junit.Test;

public class Part1OfSuiteTest {

	@Test
	public void testOk() {
		Assert.assertEquals("OK " + "-" + " test in suite", "OK - test in suite");
	}

}
