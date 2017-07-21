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

import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;

public class IdeaPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("provideModuleCompileArtifacts")
            .impl(() -> new ModuleArtifactDependencyTask("compileArtifacts"))
            .provides("allModulesCompileArtifacts", true)
            .importFromModules("compileArtifacts")
            .desc("") // TODO
            .register();

        task("provideModuleTestArtifacts")
            .impl(() -> new ModuleArtifactDependencyTask("testArtifacts"))
            .provides("allModulesTestArtifacts", true)
            .importFromModules("testArtifacts")
            .desc("") // TODO
            .register();

        task("configureIdea")
            .impl(() -> new IdeaTask(getModuleConfig()))
            .provides("idea")
            .uses("allModulesCompileArtifacts", "allModulesTestArtifacts")
            .desc("Creates .iml file and .idea directory for IntelliJ IDEA.")
            .register();
    }

}
