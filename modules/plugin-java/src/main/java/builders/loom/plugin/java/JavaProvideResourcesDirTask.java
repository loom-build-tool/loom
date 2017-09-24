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
import builders.loom.api.product.GenericProduct;
import builders.loom.api.product.Product;
import builders.loom.util.FileUtil;
import builders.loom.util.ProductChecksumUtil;

public class JavaProvideResourcesDirTask extends AbstractModuleTask {

    private final Path srcFragmentDir;

    public JavaProvideResourcesDirTask(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                srcFragmentDir = LoomPaths.RES_MAIN;
                break;
            case TEST:
                srcFragmentDir = LoomPaths.RES_TEST;
                break;
            default:
                throw new IllegalStateException("Unknown compileTarget " + compileTarget);
        }
    }

    @Override
    public TaskResult run(final boolean skip) throws Exception {
        final Path srcDir = getBuildContext().getPath().resolve(srcFragmentDir);
        final List<Path> srcFiles = findSources(srcDir);

        if (srcFiles.isEmpty()) {
            return TaskResult.empty();
        }

        return TaskResult.ok(newProduct(srcDir, srcFiles));
    }

    private List<Path> findSources(final Path srcDir) throws IOException {
        if (FileUtil.isDirAbsentOrEmpty(srcDir)) {
            return Collections.emptyList();
        }

        return Files
            .find(srcDir, Integer.MAX_VALUE, (path, attr) -> attr.isRegularFile())
            .collect(Collectors.toList());
    }

    private static Product newProduct(final Path srcDir, final List<Path> srcFiles) {
        return new GenericProduct("srcDir", srcDir.toString(),
            ProductChecksumUtil.calcChecksum(srcFiles), null);
    }

}
