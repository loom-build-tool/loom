package jobt;

import static org.junit.Assert.assertEquals;

import jobt.plugin.TaskStatus;

public class XooTest {

    public void test() {
        assertEquals("SKIP", TaskStatus.SKIP.name());
    }

}
