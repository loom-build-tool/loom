package jobt.config;

import jobt.plugin.Project;

public class ProjectImpl implements Project {

    private String group;
    private String archivesBaseName;
    private String version;

    @Override
    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    @Override
    public String getArchivesBaseName() {
        return archivesBaseName;
    }

    public void setArchivesBaseName(final String archivesBaseName) {
        this.archivesBaseName = archivesBaseName;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

}
