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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.TaskResult;
import builders.loom.api.product.SourceTreeProduct;

public class JavaProvideSourceDirModuleTask extends AbstractModuleTask {

    private static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    private static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");

    private final CompileTarget compileTarget;

    public JavaProvideSourceDirModuleTask(final CompileTarget compileTarget) {
        this.compileTarget = compileTarget;
    }

    @Override
    public TaskResult run() throws Exception {
        final Path path = srcPath();

        if (!Files.isDirectory(path)) {
            return completeSkip();
        }

        final List<Path> sourceFiles = Files.walk(srcPath())
            .filter(Files::isRegularFile)
            .collect(Collectors.toList());

        final List<Path> illegalFiles = sourceFiles.stream()
            .filter(f -> !f.toString().endsWith(".java"))
            .collect(Collectors.toList());

        if (!illegalFiles.isEmpty()) {
            throw new IllegalStateException("Found files with other suffix than .java: "
                + illegalFiles);
        }

        return completeOk(new SourceTreeProduct(path, sourceFiles));
    }

    private Path srcPath() {
        switch (compileTarget) {
            case MAIN:
                return getBuildContext().getPath().resolve(SRC_MAIN_PATH);
            case TEST:
                return getBuildContext().getPath().resolve(SRC_TEST_PATH);
            default:
                throw new IllegalStateException();
        }
    }

}
