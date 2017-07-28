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

package builders.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import builders.loom.api.GlobalBuildContext;
import builders.loom.api.Module;

public class ModuleRegistry {

    private final List<Module> modules = new ArrayList<>();

    public void register(final Module module) {
        if (module.getModuleName().equals(GlobalBuildContext.GLOBAL_MODULE_NAME)) {
            throw new IllegalArgumentException(
                "Conflicting module name " + module.getModuleName());
        }
        if (lookup(module.getModuleName()).isPresent()) {
            throw new IllegalStateException(
                "A module with name " + module.getModuleName() + " already exists");
        }
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Optional<Module> lookup(final String moduleName) {
        return modules.stream()
            .filter(m -> m.getModuleName().equals(moduleName))
            .findFirst();
    }

}
