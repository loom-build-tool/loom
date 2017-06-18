package jobt.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

public final class Util {

    private static final RuntimeMXBean MX_BEAN = ManagementFactory.getRuntimeMXBean();

    private static final char EXTENSION_SEPARATOR = '.';

    private static final char UNIX_SEPARATOR = '/';

    private static final char WINDOWS_SEPARATOR = '\\';

    private Util() {
    }

    public static URL toUrl(final Path file) {
        try {
            return file.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the uptime of the Java virtual machine in milliseconds.
     */
    public static long jvmUptime() {
        return MX_BEAN.getUptime();
    }

    public static String getFileExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        final int index = indexOfExtension(filename);
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    public static int indexOfExtension(final String filename) {
        if (filename == null) {
            return -1;
        }
        final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? -1 : extensionPos;
    }

    public static int indexOfLastSeparator(final String filename) {
        if (filename == null) {
            return -1;
        }
        final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

}
