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

import static org.junit.Assert.assertEquals;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class StringReplaceUtilTest {

    @Test
    public void simple() {
        assertEquals("Hello foo!",
            replace("Hello ${name}!"));
    }

    @Test
    public void defaultVal() {
        assertEquals("Hello foo! You are 21 years old.",
            replace("Hello ${name}! You are ${age:21} years old."));
    }

    @Test
    public void defaultEmpty() {
        assertEquals("Hello foo! You are  years old.",
            replace("Hello ${name}! You are ${age:} years old."));
    }

    @Test(expected = UncheckedIOException.class)
    public void missingDefault() {
        replace("Hello ${name}! You are ${age} years old.");
    }

    private String replace(final String tpl) {
        final Map<String, String> properties = Map.of("name", "foo");
        return StringReplaceUtil.replaceString(tpl,
            (k) -> Optional.ofNullable(properties.get(k)));
    }

}
