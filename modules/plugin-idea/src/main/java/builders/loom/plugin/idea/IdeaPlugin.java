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
        task("configureIdea")
            .impl(IdeaTask::new)
            .provides("idea")
            .usesOptionally("cleanIdea")
            .importFromAllModules("compileArtifacts", "testArtifacts")
            .desc("Creates .idea directory and .iml file(s) for IntelliJ IDEA.")
            .register();

        task("configureCleanIdea")
            .impl(IdeaCleanTask::new)
            .provides("cleanIdea")
            .desc("Cleans (removes) .idea directory and .iml file(s).")
            .register();
    }

}
