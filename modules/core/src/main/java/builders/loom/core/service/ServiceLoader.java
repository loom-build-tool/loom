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

package builders.loom.core.service;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.DependencyResolverService;
import builders.loom.api.DownloadProgressEmitter;
import builders.loom.api.LoomPaths;
import builders.loom.api.Service;
import builders.loom.core.DownloadProgressEmitterBridge;
import builders.loom.core.ProgressMonitor;
import builders.loom.core.RuntimeConfigurationImpl;
import builders.loom.core.Version;
import builders.loom.core.misc.ExtensionLoader;
import builders.loom.util.SystemUtil;

public class ServiceLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceLoader.class);

    private static final Map<String, String> SERVICES = Map.of(
        "service-maven", "builders.loom.service.maven.MavenService"
    );

    private final Path loomBaseDir = SystemUtil.determineLoomBaseDir();
    private final RuntimeConfigurationImpl runtimeConfiguration;
    private final DownloadProgressEmitter downloadProgressEmitter;
    private final ServiceRegistryImpl serviceRegistry;

    public ServiceLoader(final RuntimeConfigurationImpl runtimeConfiguration,
                         final ProgressMonitor progressMonitor,
                         final ServiceRegistryImpl serviceRegistry) {
        this.runtimeConfiguration = runtimeConfiguration;
        downloadProgressEmitter = new DownloadProgressEmitterBridge(progressMonitor);
        this.serviceRegistry = serviceRegistry;
    }

    public void initServices() {
        for (final String service : SERVICES.keySet()) {
            initService(service);
        }
    }

    private void initService(final String serviceName) {
        final Service service = getService(serviceName);
        service.setRuntimeConfiguration(runtimeConfiguration);

        service.setRepositoryPath(LoomPaths.loomDir(runtimeConfiguration.getProjectBaseDir())
            .resolve(Paths.get(Version.getVersion(), serviceName)));

        if (service instanceof DependencyResolverService) {
            final DependencyResolverService drs = (DependencyResolverService) service;
            drs.setDownloadProgressEmitter(downloadProgressEmitter);
            serviceRegistry.setDependencyResolverService(drs);
        } else {
            throw new IllegalStateException("Unknown service type: " + service.getClass());
        }

        service.init();

        LOG.info("Service {} initialized", serviceName);
    }

    private Service getService(final String serviceName) {
        final String serviceClassname = SERVICES.get(serviceName);
        final Class<?> serviceClass =
            ExtensionLoader.loadExtension(loomBaseDir, serviceName, serviceClassname);

        try {
            return (Service) serviceClass.getDeclaredConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException
            | InvocationTargetException e) {
            throw new IllegalStateException("Error initializing Service " + serviceName, e);
        }
    }

}
