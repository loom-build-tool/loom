package builders.loom.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

public class Hasher {

    private static final int MASK = 0xff;

    private final MessageDigest messageDigest = getMessageDigest();

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
