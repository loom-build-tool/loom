package jobt.plugin.java;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ResourceFilteringOutputStream extends FilterOutputStream {

    private final Map<String, String> resourceFilterVariables;
    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private int filtering;

    public ResourceFilteringOutputStream(final OutputStream out,
                                         final Map<String, String> resourceFilterVariables) {
        super(out);
        this.resourceFilterVariables = resourceFilterVariables;
    }

    @SuppressWarnings("checkstyle:nestedifdepth")
    @Override
    public void write(final int b) throws IOException {
        if (filtering == 0) {
            if (b == '$') {
                filtering = 1;
                buf.write(b);
            } else {
                out.write(b);
            }
        } else if (filtering == 1) {
            if (b == '{') {
                filtering = 2;
                buf.write(b);
            } else {
                flushBuffer();
                out.write(b);
            }
        } else {
            buf.write(b);

            if (b == '}') {
                filtering = 0;

                final String placeholder = buf.toString("UTF-8")
                    .substring(2, buf.size() - 1);
                final String resource = resourceFilterVariables.get(placeholder);
                if (resource != null) {
                    out.write(resource.getBytes(StandardCharsets.UTF_8));
                } else {
                    buf.writeTo(out);
                }

                buf.reset();
            }
        }
    }

    private void flushBuffer() throws IOException {
        filtering = 0;
        buf.writeTo(out);
        buf.reset();
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        super.flush();
    }

}
