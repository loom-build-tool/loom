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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public final class IOUtil {

    private static final int BUF_SIZE = 8192;

    private IOUtil() {
    }

    public static byte[] toByteArray(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(final InputStream from, final OutputStream to)
        throws IOException {

        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        final byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        int cnt;
        while ((cnt = from.read(buf)) != -1) {
            to.write(buf, 0, cnt);
            total += cnt;
        }
        return total;
    }

}
