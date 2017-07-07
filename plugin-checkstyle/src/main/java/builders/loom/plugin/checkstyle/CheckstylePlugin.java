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
import builders.loom.api.PluginSettings;

public class CheckstylePlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("checkstyleMain")
            .impl(() -> new CheckstyleTask(CompileTarget.MAIN))
            .provides("checkstyleMainReport")
            .uses("source", "compileDependencies")
            .register();

        task("checkstyleTest")
            .impl(() -> new CheckstyleTask(CompileTarget.TEST))
            .provides("checkstyleTestReport")
            .uses("testSource", "testDependencies")
            .register();

        goal("check")
            .requires("checkstyleMainReport", "checkstyleTestReport")
            .register();
    }

}
