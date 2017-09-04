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

package builders.loom.cli;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JPrintStream extends OutputStream {

    private static final Logger LOG = LoggerFactory.getLogger(SLF4JPrintStream.class);

    private final StringBuilder sb = new StringBuilder();
    private final boolean error;

    SLF4JPrintStream(final boolean error) {
        this.error = error;
    }

    @Override
    public void write(final int b) throws IOException {
        switch (b) {
            case '\r':
                // ignore
                break;
            case '\n':
                flush();
                break;
            default:
                sb.append((char) b);
                break;
        }
    }

    @Override
    public void flush() throws IOException {
        if (sb.length() > 0) {
            log();
            sb.delete(0, sb.length());
        }
    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    private void log() {
        if (error) {
            LOG.error("STDERR (Please replace System.err with Logging!): {}", sb);
        } else {
            LOG.info("STDOUT (Please replace System.out with Logging!): {}", sb);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }

}
