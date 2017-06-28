package jobt.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class JobtPaths {

    public static final Path PROJECT_DIR;
    public static final Path SRC_MAIN_PATH;
    public static final Path SRC_TEST_PATH ;
    public static final Path BUILD_MAIN_PATH;
    public static final Path BUILD_TEST_PATH;
    public static final Path REPORT_PATH;

    static {
        final Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        PROJECT_DIR = currentDir;

        SRC_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("src", "main", "java"));
        SRC_TEST_PATH = PROJECT_DIR.resolve(Paths.get("src", "test", "java"));
        BUILD_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("jobtbuild", "classes", "main"));
        BUILD_TEST_PATH = PROJECT_DIR.resolve(Paths.get("jobtbuild", "classes", "test"));
        REPORT_PATH = PROJECT_DIR.resolve(Paths.get("jobtbuild", "reports"));

        checkState(Files.exists(currentDir), "Invalid current directory");
    }


    private static void checkState(final boolean expression, final Object errorMessage) {
        if (!expression) {
            throw new IllegalStateException(String.valueOf(errorMessage));
        }
    }

}
