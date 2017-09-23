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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.SourceTreeProduct;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class JavaProvideSourceDirTask extends AbstractModuleTask {

    private final Path srcFragmentDir;

    public JavaProvideSourceDirTask(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                srcFragmentDir = LoomPaths.SRC_MAIN;
                break;
            case TEST:
                srcFragmentDir = LoomPaths.SRC_TEST;
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public TaskResult run() throws Exception {
        final Path srcDir = getBuildContext().getPath().resolve(srcFragmentDir);
        final List<Path> srcFiles = findSources(srcDir);

        if (srcFiles.isEmpty()) {
            return TaskResult.empty();
        }

        validateFiles(srcFiles);

        return TaskResult.ok(new SourceTreeProduct(srcDir, srcFiles, ProductChecksumUtil.calcChecksum(srcFiles)));
    }

    private List<Path> findSources(final Path srcDir) throws IOException {
        if (FileUtil.isDirAbsentOrEmpty(srcDir)) {
            return Collections.emptyList();
        }

        return Files
            .find(srcDir, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())
            .collect(Collectors.toList());
    }

    private void validateFiles(final List<Path> srcFiles) {
        final List<Path> illegalFiles = srcFiles.stream()
            .filter(f -> !f.toString().endsWith(".java"))
            .collect(Collectors.toList());

        if (!illegalFiles.isEmpty()) {
            throw new IllegalStateException("Found files with other suffix than .java: "
                + illegalFiles);
        }
    }

}
