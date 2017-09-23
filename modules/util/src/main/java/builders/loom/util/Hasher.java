/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.util;

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

    public static String bytesToHex(final byte[] hash) {
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
