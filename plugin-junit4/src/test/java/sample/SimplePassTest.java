package sample;

import org.junit.Assert;
import org.junit.Test;

public class SimplePassTest {

	@Test
	public void pass1() {
		Assert.assertEquals(42, 40 + 2);
	}

	@Test
	public void pass2() {
		Assert.assertEquals(5*5, 3*3 + 4*4);
	}

}
