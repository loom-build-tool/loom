package builders.loom.api.product;

import java.nio.file.Path;

/**
 * E.g. a "jar" file.
 *
 */
public final class AssemblyProduct implements Product {

    private final Path assemblyFile;

    public AssemblyProduct(final Path assemblyFile) {
        this.assemblyFile = assemblyFile;
    }

    public Path getAssemblyFile() {
        return assemblyFile;
    }

}
