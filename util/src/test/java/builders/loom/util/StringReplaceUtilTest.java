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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringReplaceUtilTest {

    @Test
    public void test() {
        final String tpl = "Hello ${name}! You are ${age} years old and have ${cntKids} kids.";

        final Map<String, String> properties = new HashMap<>();
        properties.put("name", "foo");
        properties.put("age", "100");
        final Function<String, Optional<String>> replaceFunction =
            (k) -> Optional.ofNullable(properties.get(k));

        assertEquals("Hello foo! You are 100 years old and have ${cntKids} kids.",
            StringReplaceUtil.replaceString(tpl, replaceFunction));
    }

}
