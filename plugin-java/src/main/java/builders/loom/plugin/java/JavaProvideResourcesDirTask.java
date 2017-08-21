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

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.CompileTarget;
import builders.loom.api.LoomPaths;
import builders.loom.api.TaskResult;
import builders.loom.api.product.ResourcesTreeProduct;
import builders.loom.util.Hasher;

public class JavaProvideResourcesDirTask extends AbstractModuleTask {

    private final CompileTarget compileTarget;

    public JavaProvideResourcesDirTask(final CompileTarget compileTarget) {
        this.compileTarget = compileTarget;
    }

    @Override
    public TaskResult run() throws Exception {
        final Path path = resourcesPath();

        if (!Files.isDirectory(path)) {
            return completeEmpty();
        }

        final List<Path> resourceFiles = new ArrayList<>();
        final Hasher hasher = new Hasher();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                resourceFiles.add(file);
                hasher
                    .putString(file.toString())
                    .putLong(attrs.size())
                    .putLong(attrs.lastModifiedTime().toMillis());

                return FileVisitResult.CONTINUE;
            }

        });

        if (resourceFiles.isEmpty()) {
            return completeEmpty();
        }

        return completeOk(new ResourcesTreeProduct(path, resourceFiles, hasher.stringHash()));
    }

    private Path resourcesPath() {
        switch (compileTarget) {
            case MAIN:
                return getBuildContext().getPath().resolve(LoomPaths.RES_MAIN);
            case TEST:
                return getBuildContext().getPath().resolve(LoomPaths.RES_TEST);
            default:
                throw new IllegalStateException();
        }
    }

}
