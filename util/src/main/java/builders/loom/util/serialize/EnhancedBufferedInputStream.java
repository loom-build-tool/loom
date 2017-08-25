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
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("checkstyle:magicnumber")
class EnhancedBufferedInputStream extends BufferedInputStream {

    EnhancedBufferedInputStream(final InputStream in) {
        super(in);
    }

    byte[] read(final int cnt) throws IOException {
        final byte[] buf = new byte[cnt];
        if (read(buf, 0, cnt) != cnt) {
            throw new IOException("Can't read " + cnt + " bytes from stream");
        }
        return buf;
    }

    int readInt() throws IOException {
        return ByteBuffer.wrap(read(4)).getInt();
    }

    String readString(final int len) throws IOException {
        return new String(read(len), StandardCharsets.UTF_8);
    }

}
