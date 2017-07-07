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

package builders.loom.plugin.pmd;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.CompileTarget;

public class PmdPlugin extends AbstractPlugin<PmdPluginSettings> {

    public PmdPlugin() {
        super(new PmdPluginSettings());
    }

    @Override
    public void configure() {
        task("pmdMain")
            .impl(() -> new PmdTask(getBuildConfig(), getPluginSettings(), CompileTarget.MAIN))
            .provides("pmdMainReport")
            .uses("source", "compileDependencies")
            .register();

        task("pmdTest")
            .impl(() -> new PmdTask(getBuildConfig(), getPluginSettings(), CompileTarget.TEST))
            .provides("pmdTestReport")
            .uses("testSource", "testDependencies")
            .register();

        goal("check")
            .requires("pmdMainReport", "pmdTestReport")
            .register();
    }

}
