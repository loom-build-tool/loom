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

import java.nio.file.Path;
import java.nio.file.Paths;

import builders.loom.api.AbstractTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.SourceTreeProduct;

public class JavaProvideSourceDirTask extends AbstractTask {

    private static final Path SRC_MAIN_PATH = Paths.get("src", "main", "java");
    private static final Path SRC_TEST_PATH = Paths.get("src", "test", "java");

    private final CompileTarget compileTarget;

    public JavaProvideSourceDirTask(final CompileTarget compileTarget) {
        this.compileTarget = compileTarget;
    }

    @Override
    public TaskStatus run() throws Exception {
        return complete(TaskStatus.OK);
    }

    private TaskStatus complete(final TaskStatus status) {
        switch (compileTarget) {
            case MAIN:
                getProvidedProducts().complete("source", new SourceTreeProduct(SRC_MAIN_PATH));
                return status;
            case TEST:
                getProvidedProducts().complete("testSource", new SourceTreeProduct(SRC_TEST_PATH));
                return status;
            default:
                throw new IllegalStateException();
        }
    }
}
