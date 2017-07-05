package builders.loom.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LoomPaths {

    public static final Path PROJECT_DIR;
    public static final Path SRC_MAIN_PATH;
    public static final Path SRC_TEST_PATH;
    public static final Path BUILD_MAIN_PATH;
    public static final Path BUILD_TEST_PATH;
    public static final Path REPORT_PATH;

    static {
        final Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        PROJECT_DIR = currentDir;

        SRC_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("src", "main", "java"));
        SRC_TEST_PATH = PROJECT_DIR.resolve(Paths.get("src", "test", "java"));
        BUILD_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "classes", "main"));
        BUILD_TEST_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "classes", "test"));
        REPORT_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "reports"));

        checkState(Files.exists(currentDir), "Invalid current directory");
    }

    private LoomPaths() {
    }

    private static void checkState(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

}
