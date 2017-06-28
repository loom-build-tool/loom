package jobt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;

public final class Hasher {

    private static final int MASK = 0xff;

    private Hasher() {
    }

    public static String hash(final Collection<String> strings) {
        final MessageDigest digest = getMessageDigest();

        for (final String string : strings) {
            digest.update(string.getBytes(StandardCharsets.UTF_8));
        }

        return bytesToHex(digest.digest());
    }

    public static byte[] hash(final byte[] data) {
        return getMessageDigest().digest(data);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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

}
