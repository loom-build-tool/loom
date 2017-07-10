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

package builders.loom.plugin.mavenresolver;

import java.nio.file.Path;

public final class MavenResolverSingleton {

    private static volatile MavenResolver instance;

    private MavenResolverSingleton() {
    }

    public static MavenResolver getInstance(final MavenResolverPluginSettings pluginSettings,
                                            final Path cacheDir) {

        if (instance == null) {
            synchronized (MavenResolverSingleton.class) {
                if (instance == null) {

                    final ProgressIndicator progressIndicator =
                        new ProgressIndicator("mavenResolver");

                    instance = new MavenResolver(progressIndicator,
                        pluginSettings.getRepositoryUrl(), cacheDir);
                }
            }
        }

        return instance;
    }

}
