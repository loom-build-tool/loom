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
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCacherImpl implements FileCacher {

    private final Path cacheFile;

    public FileCacherImpl(final Path cacheDir, final String type) {
        cacheFile = cacheDir.resolve("java-" + type + ".cache");
    }

    @Override
    @SuppressWarnings("checkstyle:returncount")
    public boolean filesCached(final List<Path> srcPaths) throws IOException {
        final Map<String, Long> cachedFiles = readCache();

        if (srcPaths.size() != cachedFiles.size()) {
            return false;
        }

        for (final Path srcPath : srcPaths) {
            final Long cachedMtime = cachedFiles.remove(srcPath.toString());
            if (cachedMtime == null) {
                return false;
            }
            final FileTime lastModifiedTime = Files.getLastModifiedTime(srcPath);
            if (!cachedMtime.equals(lastModifiedTime.toMillis())) {
                return false;
            }
        }

        return cachedFiles.isEmpty();
    }

    private Map<String, Long> readCache() throws IOException {
        if (Files.notExists(cacheFile)) {
            return Collections.emptyMap();
        }

        final List<String> strings = Files.readAllLines(cacheFile);
        return strings.stream()
            .map(s -> s.split("\t"))
            .collect(Collectors.toMap(split -> split[0], split -> Long.valueOf(split[1])));
    }

    @Override
    public void cacheFiles(final List<Path> srcPaths) throws IOException {
        final Path parent = cacheFile.getParent();

        if (parent == null) {
            // keep Findbugs happy
            throw new IllegalStateException();
        }

        Files.createDirectories(parent);

        final Map<String, Long> cachedFiles = new HashMap<>();

        for (final Path srcFile : srcPaths) {
            cachedFiles.put(srcFile.toString(), Files.getLastModifiedTime(srcFile).toMillis());
        }

        final List<String> stringStream = cachedFiles.entrySet().stream()
            .map(e -> e.getKey() + "\t" + e.getValue())
            .collect(Collectors.toList());

        Files.write(cacheFile, stringStream);
    }

}
