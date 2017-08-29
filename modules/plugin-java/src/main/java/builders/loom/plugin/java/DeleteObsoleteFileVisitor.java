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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import builders.loom.util.FileUtil;

class DeleteObsoleteFileVisitor extends SimpleFileVisitor<Path> {

    private final Path sourceBasePath;
    private final KeyValueCache cache;
    private Path targetBasePath;

    DeleteObsoleteFileVisitor(final Path sourceBasePath, final KeyValueCache cache) {
        this.sourceBasePath = sourceBasePath;
        this.cache = cache;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {

        if (targetBasePath == null) {
            targetBasePath = dir;
        } else if (Files.notExists(sourceBasePath.resolve(targetBasePath.relativize(dir)))) {
            FileUtil.deleteDirectoryRecursively(dir, true);
            return FileVisitResult.SKIP_SUBTREE;
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {

        final Path relativizedFile = targetBasePath.relativize(file);

        if (Files.notExists(sourceBasePath.resolve(relativizedFile))) {
            cache.remove(relativizedFile.toString());
            Files.delete(file);
        }

        return FileVisitResult.CONTINUE;
    }

}
