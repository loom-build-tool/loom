package builders.loom.util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

// TODO
public class SkipChecksumUtils {

    public static String jvmVersion() {
        return "JVM " + Runtime.version().toString();
    }

    public static String file(final Path file) {
        return Hashing.hash(file);
    }

    public static String always() {
        return "SKIP";
    }

    private static String never() {
        return UUID.randomUUID().toString();
    }

    public static String skipOnNull(final String str) {
        if (str == null) {
            return always();
        }

        return never();
    }

    public static String collection(final Collection<String> compileDependencies) {
        return compileDependencies.stream()
            .sorted()
            .collect(Collectors.joining(";"));
    }


    // TODO
    // config ?
    // config files (e.g. checkstyle.xml)
    // setup: (e.g. jdk version)
    // ENV
    // systemproperties


}
