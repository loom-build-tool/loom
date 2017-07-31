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

package builders.loom.api;

import java.nio.file.Path;

public class GlobalBuildContext implements BuildContext {

    public static final String GLOBAL_MODULE_NAME = "global";

    @Override
    public String getModuleName() {
        return GLOBAL_MODULE_NAME;
    }

    @Override
    public Path getPath() {
        return LoomPaths.PROJECT_DIR;
    }

    @Override
    public BuildConfig getConfig() {
        return new GlobalBuildConfig();
    }

}
