package builders.loom.api.product;

import java.nio.file.Path;

public class ModulePathProduct {

    private final String moduleName;
    private final Path modulePath;

    public ModulePathProduct(final String moduleName, final Path modulePath) {
        this.moduleName = moduleName;
        this.modulePath = modulePath;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getModulePath() {
        return modulePath;
    }

    @Override
    public String toString() {
        return "ModulePathProduct{"
            + "moduleName='" + moduleName + '\''
            + ", modulePath=" + modulePath
            + '}';
    }

}
