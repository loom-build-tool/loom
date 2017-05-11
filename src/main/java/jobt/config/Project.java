package jobt.config;

public class Project {

    private String group;
    private String archivesBaseName;
    private String version;

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public String getArchivesBaseName() {
        return archivesBaseName;
    }

    public void setArchivesBaseName(final String archivesBaseName) {
        this.archivesBaseName = archivesBaseName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

}
