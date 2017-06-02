package jobt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jobt.api.TaskStatus;

public class FooTest {

    @Test
    public void test() {
        assertEquals("OK", TaskStatus.OK.name());
    }

    @Test
    public void test2() {
        assertEquals("FAIL", TaskStatus.FAIL.name());
    }

}
