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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SimpleSerializerTest {

    @Test
    public void test() throws Exception {
        final List<List<String>> records = List.of(
            Arrays.asList("foo", "bar"),
            Arrays.asList(null, ""),
            Arrays.asList("", null),
            Arrays.asList(null, null)
        );

        final Path file = Files.createTempFile("SimpleSerializerTest", null);

        SimpleSerializer.write(file, records, Record::new);

        final List<Record> recordList = new ArrayList<>();
        SimpleSerializer.read(file, recordList::add);

        assertEquals(4, recordList.size());
        final Iterator<Record> it = recordList.iterator();
        assertEquals(Arrays.asList("foo", "bar"), it.next().getFields());
        assertEquals(Arrays.asList(null, ""), it.next().getFields());
        assertEquals(Arrays.asList("", null), it.next().getFields());
        assertEquals(Arrays.asList(null, null), it.next().getFields());
    }

}
