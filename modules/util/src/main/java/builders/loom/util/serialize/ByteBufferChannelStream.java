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

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("checkstyle:magicnumber")
class ByteBufferChannelStream implements Flushable {

    private final ByteBuffer buf = ByteBuffer.allocate(8192);
    private final ByteChannel channel;

    ByteBufferChannelStream(final ByteChannel channel) {
        this.channel = channel;
    }

    void put(final ByteBuffer src) throws IOException {
        ensureCapacity(src.remaining()).put(src);
    }

    void put(final byte b) throws IOException {
        ensureCapacity(1).put(b);
    }

    void put(final byte[] bytes) throws IOException {
        ensureCapacity(bytes.length).put(bytes);
    }

    void put(final String s) throws IOException {
        put(s.getBytes(StandardCharsets.UTF_8));
    }

    void putInt(final int i) throws IOException {
        ensureCapacity(4).putInt(i);
    }

    private ByteBuffer ensureCapacity(final int cap) throws IOException {
        if (cap > buf.limit()) {
            throw new IOException("Requested capacity " + cap + " is over limit of "
                + buf.limit());
        }

        if (buf.remaining() < cap) {
            buf.flip();

            do {
                channel.write(buf);
            } while (buf.capacity() - buf.remaining() < cap);


            if (buf.hasRemaining()) {
                buf.compact();
            } else {
                buf.clear();
            }
        }
        return buf;
    }

    @Override
    public void flush() throws IOException {
        buf.flip();
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

}
