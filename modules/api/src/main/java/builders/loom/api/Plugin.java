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

/**
 * <strong>Performance consideration:</strong> A plugin is registered, if specified in the build
 * configuration &ndash; even if it isn't used in the build process. Ensure quick initialization and
 * put time-consuming code in the {@link Task}.
 *
 * @see AbstractPlugin
 */
public interface Plugin {

    void setName(final String pluginName);

    void setTaskRegistry(TaskRegistry taskRegistry);

    default PluginSettings getPluginSettings() {
        return null;
    }

    default void setServiceRegistry(final ServiceRegistry serviceRegistry) {
    }

    default void setRuntimeConfiguration(final RuntimeConfiguration runtimeConfiguration) {
    }

    default void setRepositoryPath(final Path path) {
    }

    default void setDownloadProgressEmitter(final DownloadProgressEmitter emitter) {
    }

    default void configure() {
    }

}
