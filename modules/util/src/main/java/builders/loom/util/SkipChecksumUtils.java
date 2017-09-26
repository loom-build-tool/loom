package builders.loom.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// TODO
public class SkipChecksumUtils {

	public static String jvmVersion() {
		return Runtime.version().toString();
	}

	public static String file(final Path file) {
		try {
			return Hasher.bytesToHex(Hasher.hash(Files.readAllBytes(file)));
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String neverSkip() {
		return UUID.randomUUID().toString();
	}

}
