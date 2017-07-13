package builders.loom.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SystemUtilTest {

    @Test
    public void fullPath() {
        assertEquals("java", SystemUtil.getFileExtension("/path/to/Klasse.java"));
    }

    @Test
    public void singleFile() {
        assertEquals("java", SystemUtil.getFileExtension("Klasse.java"));
    }

    @Test
    public void pathWithDots() {
        assertEquals("java", SystemUtil.getFileExtension("../path/to/../pack/Klasse.java"));
    }

    @Test
    public void extensionOnly() {
        assertEquals("java", SystemUtil.getFileExtension(".java"));
    }

    @Test
    public void failEmpty() {
        SystemUtil.getFileExtension("");
    }

    @Test
    public void nullName() {
        assertNull(SystemUtil.getFileExtension(null));
    }

    @Test
    public void windowsPath() {
        assertEquals("java", SystemUtil.getFileExtension("C:\\path\\to\\Klasse.java"));
    }

}
