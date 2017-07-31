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

package builders.loom.plugin.findbugs;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.CompileTarget;

public class FindbugsPlugin extends AbstractPlugin<FindbugsPluginSettings> {

    public FindbugsPlugin() {
        super(new FindbugsPluginSettings());
    }

    @Override
    public void configure() {
        final FindbugsPluginSettings pluginSettings = getPluginSettings();

        task("findbugsMain")
            .impl(() -> new FindbugsModuleTask(pluginSettings, CompileTarget.MAIN))
            .provides("findbugsMainReport")
            .uses("source", "compileDependencies", "compilation")
            .desc("Runs FindBugs against main classes and create report.")
            .register();

        task("findbugsTest")
            .impl(() -> new FindbugsModuleTask(pluginSettings, CompileTarget.TEST))
            .provides("findbugsTestReport")
            .uses("testSource", "testDependencies", "compilation", "testCompilation")
            .desc("Runs FindBugs against test classes and create report.")
            .register();

        goal("check")
            .requires("findbugsMainReport", "findbugsTestReport")
            .register();
    }

}
