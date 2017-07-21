package builders.loom.api.product;

import java.nio.file.Path;

public class ModuleJarProduct {

    private final String moduleName;
    private final Path jarPath;

    public ModuleJarProduct(final String moduleName, final Path jarPath) {
        this.moduleName = moduleName;
        this.jarPath = jarPath;
    }

    public String getModuleName() {
        return moduleName;
    }

    public Path getJarPath() {
		return jarPath;
	}
    
}
