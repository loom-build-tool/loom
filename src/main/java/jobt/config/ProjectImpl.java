package jobt.config;

import java.io.Serializable;

import jobt.api.Project;

public class ProjectImpl implements Project, Serializable {

    private static final long serialVersionUID = 1L;

    private String groupId;
    private String artifactId;
    private String version;

    @Override
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    @Override
    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(final String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

}
