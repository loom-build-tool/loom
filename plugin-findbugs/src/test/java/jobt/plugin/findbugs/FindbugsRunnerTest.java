package jobt.plugin.findbugs;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;


public class FindbugsRunnerTest {

    @Test
    public void testNormalizePluginUrl() throws Exception {

        final URL url = new URL(
        "jar:file:/home/myuser/.jobt/binary/1.0.0/plugin-findbugs/findbugs-3.0.1.jar!/findbugs.xml");

        assertEquals("/home/myuser/.jobt/binary/1.0.0/plugin-findbugs/findbugs-3.0.1.jar", FindbugsRunner.normalizeUrl(url));
    }
}
