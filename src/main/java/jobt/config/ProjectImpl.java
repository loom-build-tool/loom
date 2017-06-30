package jobt.config;

import java.io.Serializable;

import jobt.api.Project;

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
