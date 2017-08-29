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
            .impl(() -> new PmdTask(getPluginSettings(), CompileTarget.MAIN,
                getRepositoryPath()))
            .provides("pmdMainReport")
            .uses("source", "compileDependencies")
            .desc("Runs PMD against main sources and creates report.")
            .register();

        task("pmdTest")
            .impl(() -> new PmdTask(getPluginSettings(), CompileTarget.TEST,
                getRepositoryPath()))
            .provides("pmdTestReport")
            .uses("testSource", "testDependencies")
            .desc("Runs PMD against test sources and creates report.")
            .register();

        goal("check")
            .requires("pmdMainReport", "pmdTestReport")
            .register();
    }

}
