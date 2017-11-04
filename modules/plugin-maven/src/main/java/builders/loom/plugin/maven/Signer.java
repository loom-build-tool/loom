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

package builders.loom.plugin.maven;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:classfanoutcomplexity",
    "checkstyle:classdataabstractioncoupling"})
class Signer {

    private final PGPSecretKey secretKey;
    private final PGPPrivateKey pgpPrivateKey;

    Signer(final Path keyring, final String password, final String keyId) {
        try {
            secretKey = findKeyRing(keyId, readKeyRing(keyring))
                .map(PGPSecretKeyRing::getSecretKey)
                .orElseThrow(() ->
                    new IllegalStateException("No key ring with id '" + keyId + "' found"));

            pgpPrivateKey = extractPrivateKey(password);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final PGPException e) {
            throw new IllegalStateException(e);
        }
    }

    private PGPSecretKeyRingCollection readKeyRing(final Path keyring)
        throws IOException, PGPException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(keyring))) {
            return new PGPSecretKeyRingCollection(in, new BcKeyFingerprintCalculator());
        }
    }

    private Optional<PGPSecretKeyRing> findKeyRing(final String keyId,
                                                   final PGPSecretKeyRingCollection ringColl) {
        for (final Iterator<PGPSecretKeyRing> it = ringColl.getKeyRings(); it.hasNext();) {
            final PGPSecretKeyRing ring = it.next();

            if (hexKey(ring).equalsIgnoreCase(keyId)) {
                return Optional.of(ring);
            }
        }

        return Optional.empty();
    }

    private String hexKey(final PGPSecretKeyRing ring) {
        return String.format("%08X", 0xFFFFFFFFL & ring.getSecretKey().getKeyID());
    }

    private PGPPrivateKey extractPrivateKey(final String password) throws PGPException {
        final PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(
            new BcPGPDigestCalculatorProvider()).build(password.toCharArray());
        return secretKey.extractPrivateKey(decryptor);
    }

    void sign(final Path fileToSign, final Path signFile) {
        try {
            final PGPSignatureGenerator sGen = new PGPSignatureGenerator(
                new BcPGPContentSignerBuilder(secretKey.getPublicKey().getAlgorithm(),
                    PGPUtil.SHA1));
            sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivateKey);

            final PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
            final Iterator<String> userIDs = secretKey.getPublicKey().getUserIDs();
            if (userIDs.hasNext()) {
                spGen.setSignerUserID(false, userIDs.next());
                sGen.setHashedSubpackets(spGen.generate());
            }

            final byte[] buf = new byte[8192];
            int cnt;
            try (InputStream in = Files.newInputStream(fileToSign)) {
                while ((cnt = in.read(buf)) != -1) {
                    sGen.update(buf, 0, cnt);
                }
            }

            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(signFile));
                 ArmoredOutputStream aOut = new ArmoredOutputStream(out)) {
                sGen.generate().encode(aOut);
            }
        } catch (final PGPException e) {
            throw new IllegalStateException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
