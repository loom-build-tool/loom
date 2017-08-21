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

package builders.loom.plugin.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractTask;
import builders.loom.api.Module;
import builders.loom.api.ModuleGraphAware;
import builders.loom.api.TaskResult;
import builders.loom.util.FileUtils;

public class EclipseCleanTask extends AbstractTask implements ModuleGraphAware {

    private static final Logger LOG = LoggerFactory.getLogger(EclipseCleanTask.class);

    private Map<Module, Set<Module>> moduleGraph;

    public EclipseCleanTask() {
    }

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> transitiveModuleGraph) {
        this.moduleGraph = transitiveModuleGraph;
    }

    @Override
    public TaskResult run() throws Exception {

        for (final Module module : allModules()) {
            cleanModuleProject(module);
        }

        return completeSkip();
    }

    private Set<Module> allModules() {
        return moduleGraph.keySet();
    }

    private void cleanModuleProject(final Module module)
        throws IOException, InterruptedException {

        final Path projectXml = module.getPath().resolve(".project");
        if (Files.exists(projectXml)) {
            LOG.info("Remove file {}", projectXml);
            Files.delete(projectXml);
        }

        final Path classpathFile = module.getPath().resolve(".classpath");
        if (Files.exists(classpathFile)) {
            LOG.info("Remove file {}", classpathFile);
            Files.delete(classpathFile);
        }

        final Path settingsDir = module.getPath().resolve(".settings");
        if (Files.exists(settingsDir)) {
            LOG.info("Remove directory {}", settingsDir);
            FileUtils.cleanDir(settingsDir);

        }

    }

}

