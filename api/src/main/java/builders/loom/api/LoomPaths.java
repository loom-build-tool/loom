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

import java.nio.file.Path;
import java.nio.file.Paths;

public final class LoomPaths {

    public static final Path SRC_MAIN = Paths.get("src", "main", "java");
    public static final Path RES_MAIN = Paths.get("src", "main", "resources");
    public static final Path SRC_TEST = Paths.get("src", "test", "java");
    public static final Path RES_TEST = Paths.get("src", "test", "resources");

    private static final Path MODULES_DIR = Paths.get("modules");
    private static final Path LOOM_DIR = Paths.get(".loom");
    private static final Path BUILD_DIR = Paths.get("build");
    private static final Path REPORT_DIR = Paths.get("reports");
    private static final Path TMP_DIR = Paths.get("tmp");

    private LoomPaths() {
    }

    public static Path modulesDir(final Path projectBaseDir) {
        return projectBaseDir.resolve(MODULES_DIR);
    }

    public static Path loomDir(final Path projectBaseDir) {
        return projectBaseDir.resolve(LOOM_DIR);
    }

    public static Path buildDir(final Path projectBaseDir) {
        return projectBaseDir.resolve(BUILD_DIR);
    }

    public static Path buildDir(final Path projectBaseDir,
                                final String moduleName, final String productId) {
        return buildDir(projectBaseDir).resolve(Paths.get(moduleName, productId));
    }

    public static Path reportDir(final Path projectBaseDir, final String productId) {
        return buildDir(projectBaseDir).resolve(REPORT_DIR).resolve(productId);
    }

    public static Path reportDir(final Path projectBaseDir,
                                 final String moduleName, final String productId) {
        return buildDir(projectBaseDir).resolve(REPORT_DIR)
            .resolve(Paths.get(moduleName, productId));
    }

    public static Path tmpDir(final Path projectBaseDir) {
        return loomDir(projectBaseDir).resolve(TMP_DIR);
    }

}
