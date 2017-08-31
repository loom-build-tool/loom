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

package builders.loom.cli;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class FileLockUtil {

    private FileLockUtil() {
    }

    static FileLock lock(final Path lockFile) throws IOException {
        if (Files.notExists(lockFile)) {
            Files.createFile(lockFile);
        }

        final FileChannel fileChannel = FileChannel.open(lockFile,
            StandardOpenOption.READ, StandardOpenOption.WRITE);
        final FileLock fileLock = fileChannel.tryLock();

        if (fileLock == null) {
            throw new IllegalStateException("Loom already running - locked by " + lockFile);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(lockFile);
            } catch (final IOException ignored) {
                // ignored
            }
        }));

        return fileLock;
    }

}
