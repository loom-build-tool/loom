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

package builders.loom.api;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

@SuppressWarnings({"checkstyle:visibilitymodifier", "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class AbstractPlugin<S extends PluginSettings> implements Plugin {

    private String pluginName;
    private TaskRegistry taskRegistry;
    private final S pluginSettings;
    private ServiceRegistry serviceRegistry;
    private RuntimeConfiguration runtimeConfiguration;
    private Path repositoryPath;
    private DownloadProgressEmitter downloadProgressEmitter;

    public AbstractPlugin() {
        this.pluginSettings = null;
    }

    public AbstractPlugin(final S pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public void setName(final String name) {
        this.pluginName = name;
    }

    @Override
    public void setTaskRegistry(final TaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @Override
    public S getPluginSettings() {
        return pluginSettings;
    }

    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    @Override
    public void setServiceRegistry(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    public RuntimeConfiguration getRuntimeConfiguration() {
        return runtimeConfiguration;
    }

    @Override
    public void setRepositoryPath(final Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public Path getRepositoryPath() {
        return repositoryPath;
    }

    @Override
    public void setDownloadProgressEmitter(final DownloadProgressEmitter emitter) {
        this.downloadProgressEmitter = emitter;
    }

    public DownloadProgressEmitter getDownloadProgressEmitter() {
        return downloadProgressEmitter;
    }

    protected TaskBuilder task(final String taskName) {
        return new TaskBuilder(taskName);
    }

    protected GoalBuilder goal(final String goalName) {
        return new GoalBuilder(goalName);
    }

    protected class TaskBuilder {

        private final String taskName;
        private Supplier<Task> taskSupplier;
        private String providedProduct;
        private boolean intermediateProduct;
        private Set<String> usedProducts = Collections.emptySet();
        private Set<String> importedProducts = Collections.emptySet();
        private Set<String> importedAllProducts = Collections.emptySet();
        private String description;

        public TaskBuilder(final String taskName) {
            this.taskName = taskName;
        }

        public TaskBuilder impl(final Supplier<Task> taskSupplierValue) {
            this.taskSupplier = taskSupplierValue;
            return this;
        }

        public TaskBuilder provides(final String product) {
            return provides(product, false);
        }

        public TaskBuilder provides(final String product, final boolean intermediate) {
            providedProduct = Objects.requireNonNull(product);
            intermediateProduct = intermediate;
            return this;
        }

        public TaskBuilder uses(final Collection<String> products) {
            usedProducts = new HashSet<>(Objects.requireNonNull(products));
            return this;
        }

        public TaskBuilder uses(final String... products) {
            return uses(Arrays.asList(Objects.requireNonNull(products)));
        }

        public TaskBuilder importFromModules(final Collection<String> products) {
            importedProducts = new HashSet<>(Objects.requireNonNull(products));
            return this;
        }

        public TaskBuilder importFromModules(final String... products) {
            return importFromModules(Arrays.asList(Objects.requireNonNull(products)));
        }

        public TaskBuilder importFromAllModules(final Collection<String> products) {
            importedAllProducts = new HashSet<>(Objects.requireNonNull(products));
            return this;
        }

        public TaskBuilder importFromAllModules(final String... products) {
            return importFromAllModules(Arrays.asList(Objects.requireNonNull(products)));
        }

        public TaskBuilder desc(final String taskDescription) {
            this.description = taskDescription;
            return this;
        }

        public void register() {
            taskRegistry.registerTask(pluginName, taskName, taskSupplier, providedProduct,
                intermediateProduct, usedProducts, importedProducts,
                importedAllProducts, description);
        }

    }

    protected class GoalBuilder {

        private final String goalName;
        private Set<String> usedProducts = Collections.emptySet();

        public GoalBuilder(final String goalName) {
            this.goalName = goalName;
        }

        public GoalBuilder requires(final Collection<String> products) {
            this.usedProducts = new HashSet<>(Objects.requireNonNull(products));
            return this;
        }

        public GoalBuilder requires(final String... products) {
            return requires(Arrays.asList(products));
        }

        public void register() {
            taskRegistry.registerGoal(pluginName, goalName, usedProducts);
        }

    }

}
