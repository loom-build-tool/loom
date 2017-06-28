package jobt.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class JobtPaths {

    public static final Path PROJECT_BASE;

    public static final Path SRC_MAIN;

    public static final Path SRC_TEST;

    public static final Path RES_MAIN;

    public static final Path RES_TEST;

    public static final Path BUILD_MAIN;

    public static final Path BUILD_TEST;

    public static final Path BUILD_RES_MAIN;

    public static final Path BUILD_RES_TEST;

    public static final Path CACHE_BASE;

    public static final Path CONFIG_BASE;

    public static final Path REPORT_BASE;

    static {
        final Path currentDir = Paths.get(".").toAbsolutePath().normalize();
        PROJECT_BASE = currentDir;
        SRC_MAIN = PROJECT_BASE.resolve(Paths.get("src", "main", "java"));
        SRC_TEST = PROJECT_BASE.resolve(Paths.get("src", "test", "java"));
        RES_MAIN = PROJECT_BASE.resolve(Paths.get("src", "main", "resources"));
        RES_TEST = PROJECT_BASE.resolve(Paths.get("src", "test", "resources"));
        BUILD_MAIN = PROJECT_BASE.resolve(Paths.get("jobtbuild", "classes", "main"));
        BUILD_TEST = PROJECT_BASE.resolve(Paths.get("jobtbuild", "classes", "test"));
        BUILD_RES_MAIN = PROJECT_BASE.resolve(Paths.get("jobtbuild", "resources", "main"));
        BUILD_RES_TEST = PROJECT_BASE.resolve(Paths.get("jobtbuild", "resources", "test"));
        CONFIG_BASE = PROJECT_BASE.resolve(Paths.get("config"));
        REPORT_BASE = PROJECT_BASE.resolve(Paths.get("jobtbuild", "reports"));
        CACHE_BASE = PROJECT_BASE.resolve(Paths.get(".jobt", "cache"));

    }

    private JobtPaths() {
    }

    /**
     * Creates/returns cache dir.
     *
     * @param qualifier used as directory name, e.g. "mavenresolver"
     */
    public static Path cacheDir(final String qualifier) {
        return qualifiedDir(CACHE_BASE, qualifier);
    }

    /**
     * Creates/returns config dir.
     *
     * @param qualifier used as directory name, e.g. "checkstyle"
     */
    public static Path configDir(final String qualifier) {
        return qualifiedDir(CONFIG_BASE, qualifier);
    }

    /**
     * Creates/returns report dir.
     *
     * @param qualifier used as directory name, e.g. "findbugs"
     */
    public static Path reportDir(final String qualifier) {
        return qualifiedDir(REPORT_BASE, qualifier);
    }

    public static Path sourceDir(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                return SRC_MAIN;
            case TEST:
                return SRC_TEST;
            default:
                throw new IllegalStateException();
        }
    }

    public static Path resourcesDir(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                return RES_MAIN;
            case TEST:
                return RES_TEST;
            default:
                throw new IllegalStateException();
        }
    }

    public static Path classesDir(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                return BUILD_MAIN;
            case TEST:
                return BUILD_TEST;
            default:
                throw new IllegalStateException();
        }
    }

    public static Path processedResourcesDir(final CompileTarget compileTarget) {
        switch (compileTarget) {
            case MAIN:
                return BUILD_RES_MAIN;
            case TEST:
                return BUILD_RES_TEST;
            default:
                throw new IllegalStateException();
        }
    }

    private static Path qualifiedDir(final Path baseDir, final String qualifier) {
        if (!qualifier.matches("[a-z\\-]+")) {
            throw new IllegalArgumentException(
                "Invalid qualifier format: " + qualifier);
        }

        final Path targetDir = baseDir.resolve(qualifier);
        try {
            Files.createDirectory(targetDir);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return targetDir;
    }

}
