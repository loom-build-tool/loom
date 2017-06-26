package jobt.api;

import java.nio.file.Path;

/**
 * E.g. a "jar" file.
 *
 */
public final class Assembly implements Product {

    private final Path assemblyFile;

    public Assembly(final Path assemblyFile) {
        this.assemblyFile = assemblyFile;
    }

    public Path getAssemblyFile() {
        return assemblyFile;
    }

}
