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

public abstract class AbstractModuleTask extends AbstractTask implements ModuleBuildConfigAware {

    private ModuleBuildConfig moduleConfig;

    @Override
    public void setModuleBuildConfig(final ModuleBuildConfig config) {
        this.moduleConfig = config;
    }

    public ModuleBuildConfig getModuleConfig() {
        return moduleConfig;
    }

    protected Path resolveBuildDir(final String productId, final CompileTarget compileTarget) {
        return resolveBuildDir(productId).resolve(compileTarget.name().toLowerCase());
    }

    protected Path resolveBuildDir(final String productId) {
        return LoomPaths.buildDir(getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(), productId);
    }

    protected Path resolveReportDir(final String productId, final CompileTarget compileTarget) {
        return resolveReportDir(productId).resolve(compileTarget.name().toLowerCase());
    }

    protected Path resolveReportDir(final String productId) {
        return LoomPaths.reportDir(getRuntimeConfiguration().getProjectBaseDir(),
            getBuildContext().getModuleName(), productId);
    }

}
