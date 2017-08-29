package sample;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class SetupFailedTest {

	@Before
	public void brokenSetup() {
		if (42 < 43) {
			throw new IllegalStateException("break the test setup");
		}
	}
	
	
	@Test
	public void testSuccess() throws Exception {
		assertEquals(42, 40 + 2);
	}
	
}
