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

    public static String recursiveContentChecksum(final Path path) {
        try {
            final Stream<Path> pathStream =
                Files.find(path, Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile());

            return recursiveContentChecksum(pathStream);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String recursiveContentChecksum(final Stream<Path> files) {
        final List<String> foo = files
            .sorted(Comparator.comparing(Path::toString))
            .flatMap(p -> Stream.of(Hasher.hashContent(p)))
            .collect(Collectors.toList());

        return Hasher.hash(foo);
    }

    public static String calcChecksum(final Path path) {
        final List<String> foo;
        try {
            foo = Files.find(path, Integer.MAX_VALUE, (p, attr) -> true)
                .sorted(Comparator.comparing(Path::toString))
                .flatMap(ProductChecksumUtil::hashMeta)
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return Hasher.hash(foo);
    }

    public static String calcChecksum(final List<Path> srcFiles) {
        final List<String> foo = srcFiles.stream()
            .sorted(Comparator.comparing(Path::toString))
            .flatMap(ProductChecksumUtil::hashMeta)
            .collect(Collectors.toList());

        return Hasher.hash(foo);
    }

    private static Stream<String> hashMeta(final Path f) {
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
