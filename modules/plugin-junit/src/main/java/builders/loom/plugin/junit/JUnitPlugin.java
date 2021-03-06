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

package builders.loom.plugin.junit;

import java.util.List;

import builders.loom.api.AbstractPlugin;
import builders.loom.api.PluginSettings;
import builders.loom.util.SkipChecksumUtil;

public class JUnitPlugin extends AbstractPlugin<PluginSettings> {

    @Override
    public void configure() {
        task("junit")
            .impl(JUnitTestTask::new)
            .provides("junitReport")
            .uses("testDependencies", "processedResources", "compilation",
                "processedTestResources", "testCompilation")
            .importFromModules("compilation")
            .desc("Executes tests with JUnit 5 and creates test report.")
            .skipHints(List.of(SkipChecksumUtil.jvmVersion(), () -> "Module Java version "
                + getModuleBuildConfig().getBuildSettings().getJavaPlatformVersion()))
            .register();

        goal("check")
            .requires("junitReport")
            .register();
    }

}
