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

package builders.loom.plugin.java;

import java.util.Collection;
import java.util.spi.ToolProvider;

class JarToolWrapper {

    private final ToolProvider toolProvider = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new IllegalStateException("Couldn't find jar ToolProvider"));

    @SuppressWarnings("checkstyle:regexpmultiline")
    void jar(final Collection<String> args) {
        final int result = toolProvider.run(System.out, System.err, args.toArray(new String[]{}));

        if (result != 0) {
            throw new IllegalStateException("Building jar file failed - error code: " + result);
        }
    }

}
