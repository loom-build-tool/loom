package sample;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class SkippedTest {
	
	@Test
	public void testSuccess() throws Exception {
		assertEquals(42, 40 + 2);
	}
	
	@Test
	@Ignore
	public void testIgnored() throws Exception {
		fail("should not be called");
	}

}
