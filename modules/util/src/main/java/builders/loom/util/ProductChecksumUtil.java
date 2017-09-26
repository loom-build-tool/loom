package builders.loom.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// FIXME
public class ProductChecksumUtil {

    public static String calcChecksum(final Path path) {
        return Hasher.hash(List.of(path.toString()));
    }

    public static String calcChecksum(final List<Path> srcFiles) {
        final List<String> foo = srcFiles.stream()
            .sorted(Comparator.comparing(Path::toString))
            .flatMap(ProductChecksumUtil::hash)
            .collect(Collectors.toList());

        return Hasher.hash(foo);
    }

    private static Stream<String> hash(final Path f) {
        try {
            return Stream.of(f.toString(), Long.toString(Files.size(f)),
                Long.toString(Files.getLastModifiedTime(f).toMillis()));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String calcChecksum(final Map<String, List<String>> properties) {
        final List<String> foo = new ArrayList<>();

        properties.keySet().stream().sorted().forEach(k -> {
            foo.add(k);
            foo.addAll(properties.get(k));
        });

        return Hasher.hash(foo);
    }

}
