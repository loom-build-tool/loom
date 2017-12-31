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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import builders.loom.util.Preconditions;

class DeployProperties {

    private final String releaseUrl;
    private final String snapshotUrl;
    private final String username;
    private final String password;

    private final Path keyRingFile;
    private final String keyId;
    private final String keyPassword;

    @SuppressWarnings("checkstyle:booleanexpressioncomplexity")
    DeployProperties(final Path configFile) throws IOException {
        final Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        }

        username = props.getProperty("username");
        password = props.getProperty("password");
        releaseUrl = props.getProperty("releaseUrl");
        snapshotUrl = props.getProperty("snapshotUrl");

        // all or nothing
        keyRingFile = Optional.ofNullable(props.getProperty("signing.secretKeyRingFile"))
            .map(Paths::get).orElse(null);
        keyId = props.getProperty("signing.keyId");
        keyPassword = props.getProperty("signing.password");
        Preconditions.checkState(
            (keyRingFile == null) && (keyId == null) && (keyPassword == null)
            || (keyRingFile != null) && (keyId != null) && (keyPassword != null),
            "Signing must be configured all or nothing"
        );
    }

    String getReleaseUrl() {
        return releaseUrl;
    }

    String getSnapshotUrl() {
        return snapshotUrl;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    Path getKeyRingFile() {
        return keyRingFile;
    }

    String getKeyId() {
        return keyId;
    }

    String getKeyPassword() {
        return keyPassword;
    }

    boolean isSigningEnabled() {
        return keyRingFile != null;
    }

}
