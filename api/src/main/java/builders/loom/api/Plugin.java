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

import builders.loom.api.service.ServiceLocatorRegistration;

/**
 * <strong>Performance consideration:</strong> A plugin is registered, if specified in the build
 * configuration &ndash; even if it isn't used in the build process. Ensure quick initialization and
 * put time-consuming code in the {@link Task}.
 *
 * @see AbstractPlugin
 */
public interface Plugin {

    PluginSettings getPluginSettings();

    void setTaskRegistry(TaskRegistry taskRegistry);

    void setServiceLocator(ServiceLocatorRegistration serviceLocator);

    void setBuildConfig(BuildConfig buildConfig);

    void setRuntimeConfiguration(RuntimeConfiguration runtimeConfiguration);

    void configure();

}
