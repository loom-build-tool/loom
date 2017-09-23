package builders.loom.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// FIXME
public class ProductChecksumUtil {

	public static String calcChecksum(final List<Path> srcFiles) {
		final List<String> foo = srcFiles.stream().sorted(Comparator.comparing((final Path f) -> f.toString())).flatMap(f -> hash(f)).collect(Collectors.toList());

		System.out.println("allPaths=" + foo);

		return Hasher.hash(foo);
	}

	private static Stream<String> hash(final Path f) {
		try {
			return Stream.of(f.toString(), Long.toString(Files.size(f)));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}


}
