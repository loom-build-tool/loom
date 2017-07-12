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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.ProductRepository;
import builders.loom.api.service.ServiceLocator;
import builders.loom.plugin.ConfiguredTask;
import builders.loom.plugin.TaskRegistryLookup;

public class TaskRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);

    private final TaskRegistryLookup taskRegistry;
    private final ProductRepository productRepository;
    private final ServiceLocator serviceLocator;

    public TaskRunner(final TaskRegistryLookup taskRegistry,
        final ProductRepository productRepository,
        final ServiceLocator serviceLocator) {
        this.taskRegistry = taskRegistry;
        this.productRepository = productRepository;
        this.serviceLocator = serviceLocator;
    }

    @SuppressWarnings("checkstyle:regexpmultiline")
    public Collection<ConfiguredTask> execute(final Set<String> productIds)
        throws InterruptedException, BuildException {

        final Collection<ConfiguredTask> resolvedTasks = taskRegistry.resolve(productIds);

        if (resolvedTasks.isEmpty()) {
            return Collections.emptyList();
        }

        LOG.info("Execute {}", resolvedTasks.stream()
            .map(ConfiguredTask::getName)
            .collect(Collectors.joining(", ")));

        // TODO product validate here -- to late?
        registerProducts(resolvedTasks);

        ProgressMonitor.setTasks(resolvedTasks.size());

        final JobPool jobPool = new JobPool();
        jobPool.submitAll(buildJobs(resolvedTasks));
        jobPool.shutdown();

        return resolvedTasks;
    }

    private void registerProducts(final Collection<ConfiguredTask> resolvedTasks) {
        resolvedTasks.stream()
            .map(ConfiguredTask::getProvidedProduct)
            .forEach(productRepository::createProduct);
    }

    private Collection<Job> buildJobs(final Collection<ConfiguredTask> resolvedTasks) {
        return resolvedTasks.stream()
            .map(ct -> new Job(ct.getName(), ct, productRepository, serviceLocator))
            .collect(Collectors.toList());
    }

}
