package jobt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import jobt.api.TaskStatus;

public class BarTest {

    @Test
    public void test() {
        assertEquals("SKIP", TaskStatus.SKIP.name());
    }

}
