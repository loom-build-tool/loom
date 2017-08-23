package sample;

import org.junit.Assert;
import org.junit.Test;

public class Part2OfSuiteTest {

	@Test
	public void testFail() {
		Assert.assertEquals("fail", "test in suite");
	}

}
