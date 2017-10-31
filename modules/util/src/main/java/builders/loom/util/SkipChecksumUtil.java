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

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// TODO
public final class SkipChecksumUtil {

    public static Supplier<String> jvmVersion() {
        return () -> "JVM " + Runtime.version().toString();
    }

    public static Supplier<String> file(final Path file) {
        return () -> new Hasher().putFile(file).hashHex();
    }

    public static Supplier<String> always() {
        return () -> "SKIP";
    }

    private static Supplier<String> never() {
        return () -> UUID.randomUUID().toString();
    }

    public static Supplier<String> whenNull(final String str) {
        return str == null ? always() : never();
    }

    public static Supplier<String> collection(final Collection<String> compileDependencies) {
        return () -> compileDependencies.stream()
            .sorted()
            .collect(Collectors.joining(";"));
    }


    // TODO
    // config ?
    // config files (e.g. checkstyle.xml)
    // setup: (e.g. jdk version)
    // ENV
    // systemproperties


}
