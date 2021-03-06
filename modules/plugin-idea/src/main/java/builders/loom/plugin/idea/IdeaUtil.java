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

package builders.loom.plugin.idea;

import java.nio.file.Path;

final class IdeaUtil {

    private IdeaUtil() {
    }

    static String ideaModuleName(final Path path) {
        final Path currentWorkDirName = path.toAbsolutePath().normalize().getFileName();

        if (currentWorkDirName == null) {
            throw new IllegalStateException("Can't get current working directory");
        }

        return currentWorkDirName.toString();
    }

    static Path imlFileFromPath(final Path path, final String ideaModuleName) {
        return path.resolve(ideaModuleName + ".iml");
    }

}
