package sample;

import org.junit.Assert;
import org.junit.Test;

public class SimpleFailTest {

	@Test
	public void testFail() {
		Assert.assertEquals("fail", "foo");
	}

}
