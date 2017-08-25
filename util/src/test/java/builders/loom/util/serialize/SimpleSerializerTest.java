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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SimpleSerializerTest {

    @Test
    public void test() throws Exception {
        final List<List<String>> records = List.of(
            List.of("foo", "bar"),
            List.of("faz", "baz")
        );

        final Path file = Files.createTempFile("SimpleSerializerTest", null);

        SimpleSerializer.write(file, records, Record::new);

        final List<Record> recordList = new ArrayList<>();
        SimpleSerializer.read(file, recordList::add);
        Assert.assertEquals(2, recordList.size());
        Assert.assertEquals(List.of("foo", "bar"), recordList.get(0).getFields());
        Assert.assertEquals(List.of("faz", "baz"), recordList.get(1).getFields());
    }

}
