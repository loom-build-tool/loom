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
import builders.loom.util.FileUtil;

public class EclipseCleanTask extends AbstractTask implements ModuleGraphAware {

    private static final Logger LOG = LoggerFactory.getLogger(EclipseCleanTask.class);

    private Map<Module, Set<Module>> moduleGraph;

    @Override
    public void setTransitiveModuleGraph(final Map<Module, Set<Module>> transitiveModuleGraph) {
        this.moduleGraph = transitiveModuleGraph;
    }

    @Override
    public TaskResult run() throws Exception {
        for (final Module module : listAllModules()) {
            remove(module.getPath().resolve(".project"));
            remove(module.getPath().resolve(".classpath"));
            removeDir(module.getPath().resolve(".settings"));
        }

        return completeEmpty();
    }

    private Set<Module> listAllModules() {
        return moduleGraph.keySet();
    }

    private void remove(final Path file) throws IOException {
        if (Files.exists(file)) {
            LOG.info("Remove file {}", file);
            Files.delete(file);
        }
    }

    private void removeDir(final Path dir) {
        if (Files.exists(dir)) {
            LOG.info("Remove directory {}", dir);
            FileUtil.deleteDirectoryRecursively(dir, true);
        }
    }

}
