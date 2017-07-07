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

package builders.loom.config;

import java.io.Serializable;

import builders.loom.api.Project;

class ProjectImpl implements Project, Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupId;
    private final String artifactId;
    private final String version;

    ProjectImpl(final String groupId, final String artifactId, final String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ProjectImpl{"
            + "groupId='" + groupId + '\''
            + ", artifactId='" + artifactId + '\''
            + ", version='" + version + '\''
            + '}';
    }

}
