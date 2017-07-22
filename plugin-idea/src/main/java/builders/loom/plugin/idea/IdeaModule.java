package builders.loom.plugin.idea;

public class IdeaModule {

    private final String moduleName;
    private final ModuleGroup group;
    private final String imlFileName;

    public IdeaModule(final String moduleName, final ModuleGroup group, final String imlFileName) {
        this.moduleName = moduleName;
        this.group = group;
        this.imlFileName = imlFileName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFilename() {
        return imlFileName;
    }

    public ModuleGroup getGroup() {
        return group;
    }

}
