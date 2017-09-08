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

package builders.loom.plugin.spotbugs;

import java.util.Collections;
import java.util.List;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.CompileTarget;

public class SpotBugsPlugin extends AbstractPlugin<SpotBugsPluginSettings> {

    public SpotBugsPlugin() {
        super(new SpotBugsPluginSettings());
    }

    @Override
    public void configure() {
        final SpotBugsPluginSettings pluginSettings = getPluginSettings();

        final List<String> tasksOfGoal = Collections.singletonList("spotbugsMainReport");

        task("spotbugsMain")
            .impl(() -> new SpotBugsTask(CompileTarget.MAIN, pluginSettings))
            .provides("spotbugsMainReport")
            .uses("source", "compileDependencies", "compilation")
            .desc("Runs SpotBugs against main classes and create report.")
            .register();

        if (!pluginSettings.isExcludeTests()) {
            task("spotbugsTest")
                .impl(() -> new SpotBugsTask(CompileTarget.TEST, pluginSettings))
                .provides("spotbugsTestReport")
                .uses("testSource", "testDependencies", "compilation", "testCompilation")
                .desc("Runs SpotBugs against test classes and create report.")
                .register();

            tasksOfGoal.add("spotbugsTestReport");
        }

        goal("check")
            .requires(tasksOfGoal)
            .register();
    }

}
