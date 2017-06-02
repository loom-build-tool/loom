package jobt;

import static org.junit.Assert.assertEquals;

import jobt.api.TaskStatus;

public class XooTest {

    public void test() {
        assertEquals("SKIP", TaskStatus.SKIP.name());
    }

}
