package builders.loom.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class Hasher {

    private static final int MASK = 0xff;

    private final MessageDigest messageDigest = getMessageDigest();

    public Hasher putByte(final byte data) {
        messageDigest.update(data);
        return this;
    }

    public Hasher putBytes(final byte[] data) {
        messageDigest.update(data);
        return this;
    }

    public Hasher putBytes(final byte[] data, final int off, final int len) {
        messageDigest.update(data, off, len);
        return this;
    }

    public Hasher putStrings(final Stream<String> data) {
        data.forEach(this::putString);
        return this;
    }

    public Hasher putStrings(final String... data) {
        for (final String str : data) {
            putString(str);
        }
        return this;
    }

    public Hasher putStrings(final Iterable<String> data) {
        data.forEach(this::putString);
        return this;
    }

    public Hasher putString(final String data) {
        messageDigest.update(data.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public Hasher putFile(final Path f) {
        final byte[] buf = new byte[8192];
        int cnt;

        try (final InputStream in = Files.newInputStream(f)) {
            while ((cnt = in.read(buf)) != -1) {
                messageDigest.update(buf, 0, cnt);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        return this;
    }

    public byte[] hash() {
        return messageDigest.digest();
    }

    public String hashHex() {
        return bytesToHex(hash());
    }

    private static String bytesToHex(final byte[] hash) {
        final StringBuilder hexString = new StringBuilder();
        for (final byte aHash : hash) {
            final String hex = Integer.toHexString(MASK & aHash);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
