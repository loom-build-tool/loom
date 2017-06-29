package jobt.api.product;

import java.nio.file.Path;

public final class CompilationProduct implements Product {

    private final Path classesDir;

    public CompilationProduct(final Path classesDir) {
        this.classesDir = classesDir;
    }

    public Path getClassesDir() {
        return classesDir;
    }

}
