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

package builders.loom.plugin.maven;

import builders.loom.api.PluginSettings;

public class MavenResolverPluginSettings implements PluginSettings {

    private String repositoryUrl = "https://repo.maven.apache.org/maven2/";
    private String groupAndArtifact;

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(final String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getGroupAndArtifact() {
        return groupAndArtifact;
    }

    public void setGroupAndArtifact(final String groupAndArtifact) {
        this.groupAndArtifact = groupAndArtifact;
    }

}
