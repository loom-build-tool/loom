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

package builders.loom.util.serialize;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("checkstyle:magicnumber")
class EnhancedBufferedInputStream extends BufferedInputStream {

    EnhancedBufferedInputStream(final InputStream in) {
        super(in);
    }

    byte[] read(final int cnt) throws IOException {
        final byte[] buf = new byte[cnt];
        if (read(buf, 0, cnt) != cnt) {
            throw new EOFException();
        }
        return buf;
    }

    byte readByte() throws IOException {
        final int read = read();
        if (read < 0) {
            throw new EOFException();
        }
        return (byte) read;
    }

    int readInt() throws IOException {
        final byte[] b = read(4);
        return b[0] << 24
            | (b[1] & 0xFF) << 16
            | (b[2] & 0xFF) << 8
            | (b[3] & 0xFF);
    }

    String readString(final int len) throws IOException {
        return new String(read(len), StandardCharsets.UTF_8);
    }

}
