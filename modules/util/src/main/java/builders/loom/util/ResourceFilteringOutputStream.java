/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

public class ResourceFilteringOutputStream extends FilterOutputStream {

    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    private final Function<String, Optional<String>> propertyResolver;
    private int filtering;

    public ResourceFilteringOutputStream(
        final OutputStream out,
        final Function<String, Optional<String>> propertyResolver) {

        super(out);
        this.propertyResolver = propertyResolver;
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

                if (placeholder.isEmpty()) {
                    throw new IOException("Empty placeholder not permitted");
                }

                final String resource = evaluatePlaceholder(placeholder)
                    .orElseThrow(() -> new IOException("No value found for placeholder '"
                        + placeholder + "'"));

                out.write(resource.getBytes(StandardCharsets.UTF_8));

                buf.reset();
            }
        }
    }

    private Optional<String> evaluatePlaceholder(final String placeholder) {
        final int idx = placeholder.indexOf(':');
        if (idx == -1) {
            return propertyResolver.apply(placeholder);
        }

        return propertyResolver.apply(placeholder.substring(0, idx))
            .or(() -> Optional.of(placeholder.substring(idx + 1)));
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

    @Override
    public void close() throws IOException {
        if (filtering == 2) {
            throw new IOException("Filtering is active while closing stream");
        }
        super.close();
    }

}
