package builders.loom.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class Util {

    private static final char EXTENSION_SEPARATOR = '.';

    private static final char UNIX_SEPARATOR = '/';

    private static final char WINDOWS_SEPARATOR = '\\';

    private Util() {
    }

    public static URL toUrl(final Path file) {
        if (file == null) {
            return null;
        }
        try {
            return file.toUri().toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * "com.example.Foo" -> "com/example/Foo.class"
     */
    public static String resourceNameFromClassName(final String className) {
        Objects.requireNonNull(className);
        return className.replace('.', '/') + ".class";
    }

    /**
     * "com/example/Foo.class" -> "com.example.Foo".
     */
    public static String classnameFromFilename(final String filename) {
        return
            StreamSupport.stream(
                Paths.get(filename.replaceFirst("\\.class$", "")).spliterator(), false)
            .map(Path::toString)
            .collect(Collectors.joining("."));
    }

    public static byte[] toByteArray(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
      }

    private static long copy(final InputStream from, final OutputStream to)
        throws IOException {
      Objects.requireNonNull(from);
      Objects.requireNonNull(to);
      final byte[] buf = new byte[8192];
      long total = 0;
      while (true) {
        final int r = from.read(buf);
        if (r == -1) {
          break;
        }
        to.write(buf, 0, r);
        total += r;
      }
      return total;
    }

}
