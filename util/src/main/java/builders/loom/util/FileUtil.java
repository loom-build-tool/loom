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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public final class FileUtil {

    private FileUtil() {
    }

    public static Path createOrCleanDirectory(final Path directory) throws IOException {
        if (Files.notExists(directory)) {
            return Files.createDirectories(directory);
        }

        deleteDirectoryRecursively(directory, false);

        return directory;
    }

    public static boolean isDirAbsentOrEmpty(final Path directory) throws IOException {
        return Files.notExists(directory) || isDirEmpty(directory);
    }

    public static boolean isDirEmpty(final Path directory) throws IOException {
        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static void copyFiles(final List<Path> sources, final Path dstDir) {
        try {
            for (final Path source : sources) {
                Files.copy(source, dstDir.resolve(source.getFileName()));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyFiles(final Path srcDir, final Path dstDir) {
        try {
            Files.walkFileTree(srcDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(final Path dir,
                                                         final BasicFileAttributes attrs)
                    throws IOException {
                    Files.createDirectories(dstDir.resolve(srcDir.relativize(dir)));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {

                    Files.copy(file, dstDir.resolve(srcDir.relativize(file)));
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteDirectoryRecursively(final Path rootPath, final boolean rootItself) {
        if (Files.notExists(rootPath)) {
            return;
        }

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                    throws IOException {
                    if (rootItself || !Files.isSameFile(rootPath, dir)) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
