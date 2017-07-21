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

package builders.loom.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LoomPaths {

    public static final Path PROJECT_DIR = Paths.get("").toAbsolutePath().normalize();
    public static final Path PROJECT_LOOM_PATH = PROJECT_DIR.resolve(".loom");
    public static final Path BUILD_DIR = PROJECT_DIR.resolve("loombuild");
    public static final Path REPORT_PATH = BUILD_DIR.resolve("reports");
    public static final Path BUILD_FILE = PROJECT_DIR.resolve("build.yml");

    static {
        checkState(Files.exists(PROJECT_DIR), "Invalid current directory");
    }

    private LoomPaths() {
    }

    private static void checkState(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static Path relativize(final Path path) {
        return PROJECT_DIR.relativize(path.toAbsolutePath().normalize());
    }

}
