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

import java.util.Iterator;
import java.util.List;

public final class Hashing {

    private Hashing() {
    }

    public static String hash(final Iterable<String> strings) {
        final Hasher hasher = new Hasher();
        for (final Iterator<String> iterator = strings.iterator(); iterator.hasNext();) {
            final String string = iterator.next();
            hasher.putString(string);

            if (iterator.hasNext()) {
                hasher.putByte((byte) 0);
            }

        }
        return hasher.hashHex();
    }

    public static String hash(final String... strings) {
        return hash(List.of(strings));
    }

}
