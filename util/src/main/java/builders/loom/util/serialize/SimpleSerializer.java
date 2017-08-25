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
        int recordCnt = 0;
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
                        bbs.putInt(-1);
                    }
                }

                recordCnt++;
            }

            bbs.flush();
            commit(channel, recordCnt, recordSize);
        }
    }

    // write record count/size and commit status
    private static void commit(final SeekableByteChannel channel,
                               final int recordCnt, final byte recordSize) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate(6)
            .putInt(recordCnt).put(recordSize).put(COMMITED).flip();
        channel.position(4).write(buf);
    }

    public static void read(final Path file, final Consumer<Record> consumer)
        throws IOException {

        try (final EnhancedBufferedInputStream in = new EnhancedBufferedInputStream(
            new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ)))) {

            final byte[] magic = in.read(3);
            final byte version = in.readByte();
            final int recordCnt = in.readInt();
            final byte recordSize = in.readByte();
            final byte committed = in.readByte();
            validateHeader(magic, version, committed);

            for (int r = 0; r < recordCnt; r++) {
                final String[] fields = new String[recordSize];
                for (int i = 0; i < recordSize; i++) {
                    final int fieldLength = in.readInt();
                    fields[i] = fieldLength < 0 ? null : in.readString(fieldLength);
                }
                consumer.accept(new Record(fields));
            }

        }

    }

    private static void validateHeader(final byte[] magic, final byte version,
                                       final byte committed) throws IOException {

        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new IOException("Invalid magic header");
        }

        if (version != VERSION) {
            throw new IOException("Invalid version " + version);
        }

        if (committed != COMMITED) {
            throw new IOException("Uncommitted file");
        }
    }

}
