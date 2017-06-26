package jobt.api;

import java.nio.file.Path;

public final class Compilation implements Product {

    private final Path classesDir;

    public Compilation(final Path classesDir) {
        this.classesDir = classesDir;
    }

    public Path getClassesDir() {
        return classesDir;
    }

}
