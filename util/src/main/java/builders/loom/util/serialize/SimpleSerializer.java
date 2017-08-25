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
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("checkstyle:magicnumber")
public final class SimpleSerializer {

    // Loom Simple Serializer
    private static final byte[] MAGIC_HEADER = {'L', 'S', 'S'};

    private static final byte VERSION = 1;
    private static final byte UNCOMMITED = 0;
    private static final byte COMMITED = 1;

    private SimpleSerializer() {
    }

    // TODO atomic move file? otherwise corrupted file will be read later...

    public static <T> void write(final Path file, final Iterable<T> records,
                                 final Function<T, Record> mapper) throws IOException {
        int recordCnt = -1;
        byte recordSize = -1;

        try (final SeekableByteChannel channel = Files.newByteChannel(file,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {

            final ByteBufferChannelStream bbs = new ByteBufferChannelStream(channel);

            // write header
            bbs.put(MAGIC_HEADER);
            bbs.put(VERSION);
            bbs.putInt(recordCnt);
            bbs.put(recordSize);
            bbs.put(UNCOMMITED);

            for (final T record : records) {
                final List<String> fields = mapper.apply(record).getFields();
                if (recordSize < 0) {
                    if (fields.size() > Byte.MAX_VALUE) {
                        throw new IllegalStateException("Max " + Byte.MAX_VALUE + " fields per "
                            + "record allowed");
                    }
                    recordSize = (byte) fields.size();
                } else if (recordSize != fields.size()) {
                    throw new IllegalStateException("Every record must have the same field "
                        + "count. First record had " + recordSize + " fields, now got "
                        + fields.size() + " fields");
                }

                for (final String field : fields) {
                    if (field != null) {
                        bbs.putInt(field.length());
                        bbs.put(field);
                    } else {
                        bbs.putInt(0);
                    }
                }

                recordCnt++;
            }

            bbs.flush();

            // write record count/size and commit status
            final ByteBuffer buf = ByteBuffer.allocate(6)
                .putInt(recordCnt).put(recordSize).put(COMMITED).flip();
            channel.position(4).write(buf);
        }
    }

    public static void read(final Path file, final Consumer<Record> transformer)
        throws IOException {

        try (final EnhancedBufferedInputStream in = new EnhancedBufferedInputStream(
            new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ)))) {

            final byte[] headerBytes = in.read(10);

            if (Arrays.compare(headerBytes, 0, 2, MAGIC_HEADER, 0, 2) != 0) {
                throw new IllegalStateException("Invalid magic header");
            }

            if (headerBytes[3] != VERSION) {
                throw new IllegalStateException("Invalid version " + headerBytes[3]);
            }

            if (headerBytes[9] != COMMITED) {
                throw new IllegalStateException("Uncommited file");
            }

            final ByteBuffer wrap = ByteBuffer.wrap(headerBytes, 4, 4);
            final int recordCnt = wrap.getInt();

            final byte recordSize = headerBytes[8];

            for (int r = 0; r <= recordCnt; r++) {
                final List<String> fields = new ArrayList<>();
                for (int i = 0; i < recordSize; i++) {
                    final int fieldLength = in.readInt();
                    final String fieldVal = fieldLength > 0 ? in.readString(fieldLength) : null;
                    fields.add(fieldVal);
                }
                transformer.accept(new Record(fields));
            }

        }

    }

}
