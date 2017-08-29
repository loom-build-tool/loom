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

package builders.loom.plugin.checkstyle;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.CompileTarget;

public class CheckstylePlugin extends AbstractPlugin<CheckstylePluginSettings> {

    public CheckstylePlugin() {
        super(new CheckstylePluginSettings());
    }

    @Override
    public void configure() {
        task("checkstyleMain")
            .impl(() -> new CheckstyleTask(CompileTarget.MAIN, getPluginSettings(),
                getRepositoryPath()))
            .provides("checkstyleMainReport")
            .uses("source", "compileDependencies")
            .desc("Runs Checkstyle against main sources and create report.")
            .register();

        task("checkstyleTest")
            .impl(() -> new CheckstyleTask(CompileTarget.TEST, getPluginSettings(),
                getRepositoryPath()))
            .provides("checkstyleTestReport")
            .uses("testSource", "testDependencies")
            .desc("Runs Checkstyle against test sources and create report.")
            .register();

        goal("check")
            .requires("checkstyleMainReport", "checkstyleTestReport")
            .register();
    }

}
