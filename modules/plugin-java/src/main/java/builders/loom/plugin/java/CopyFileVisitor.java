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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.util.FileUtil;
import builders.loom.util.ResourceFilteringOutputStream;

class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private static final Logger LOG = LoggerFactory.getLogger(CopyFileVisitor.class);

    private final Path targetBasePath;
    private final KeyValueCache cache;
    private final PathMatcher matcher;
    private final Function<String, Optional<String>> propertyResolver;
    private Path sourceBasePath;

    CopyFileVisitor(final Path targetBasePath, final KeyValueCache cache) throws IOException {
        this(targetBasePath, cache, null, null);
    }

    CopyFileVisitor(final Path targetBasePath, final KeyValueCache cache,
                    final String filterGlob,
                    final Function<String, Optional<String>> propertyResolver) throws IOException {

        this.targetBasePath = targetBasePath;
        this.cache = cache;
        this.propertyResolver = propertyResolver;
        Files.createDirectories(targetBasePath);

        matcher = filterGlob != null
            ? FileSystems.getDefault().getPathMatcher("glob:" + filterGlob)
            : path -> false;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {

        if (sourceBasePath == null) {
            sourceBasePath = dir;
            return FileVisitResult.CONTINUE;
        }

        final Path destPath = targetBasePath.resolve(sourceBasePath.relativize(dir));
        if (Files.exists(destPath)) {
            if (Files.isDirectory(destPath)) {
                return FileVisitResult.CONTINUE;
            }

            Files.delete(destPath);
        }

        Files.createDirectory(destPath);

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {

        final long lastModifiedTime = Files.getLastModifiedTime(file).toMillis();

        final Path relativizedFile = sourceBasePath.relativize(file);

        final String cacheKey = relativizedFile.toString();
        final Long cachedMtime = cache.get(cacheKey);
        if (cachedMtime != null && cachedMtime == lastModifiedTime) {
            return FileVisitResult.CONTINUE;
        }

        final Path destPath = targetBasePath.resolve(relativizedFile);

        if (Files.exists(destPath) && Files.isDirectory(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath, true);
        }

        if (matcher.matches(relativizedFile)) {
            LOG.debug("Copy file {} with resource filtering", file);
            try (final ResourceFilteringOutputStream out = newOut(destPath)) {
                Files.copy(file, out);
            }
        } else {
            LOG.debug("Copy file {} without resource filtering", file);
            Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING);

            // don't cache filtered files -- system properties or environment variables may change
            cache.put(cacheKey, lastModifiedTime);
        }

        return FileVisitResult.CONTINUE;
    }

    private ResourceFilteringOutputStream newOut(final Path destPath) throws IOException {
        return new ResourceFilteringOutputStream(new BufferedOutputStream(Files.newOutputStream(
            destPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)),
            propertyResolver);
    }

}
