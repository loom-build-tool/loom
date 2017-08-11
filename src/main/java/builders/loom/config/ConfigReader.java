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

package builders.loom.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import builders.loom.RuntimeConfigurationImpl;
import builders.loom.Version;
import builders.loom.api.LoomPaths;
import builders.loom.api.ModuleBuildConfig;
import builders.loom.util.Hasher;

public final class ConfigReader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);

    private ConfigReader() {
    }

    public static ModuleBuildConfig readConfig(final RuntimeConfigurationImpl runtimeConfig,
                                               final Path buildFile, final String cacheName) {

        if (!Files.isRegularFile(buildFile)) {
            throw new IllegalArgumentException("No module.yml found");
        }

        try {
            final byte[] configData = Files.readAllBytes(buildFile);

            final BuildConfigImpl buildConfig;
            if (!runtimeConfig.isCacheEnabled()) {
                buildConfig = parseConfig(configData);
                LOG.debug("Working with parsed config: {}", buildConfig);
            } else {
                buildConfig = parseAndCacheConfig(configData, cacheName);
                LOG.debug("Working with cached config: {}", buildConfig);
            }

            return buildConfig;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static BuildConfigImpl parseAndCacheConfig(final byte[] configData,
                                                       final String cacheName) throws IOException {
        final byte[] configHash = Hasher.hash(configData);

        final Path cachePath =
            LoomPaths.PROJECT_LOOM_PATH.resolve(Paths.get(Version.getVersion(), cacheName));

        Files.createDirectories(cachePath);

        final Path hashFile = cachePath.resolve(Paths.get("config.hash"));
        final Path cacheFile = cachePath.resolve(Paths.get("config.cache"));

        BuildConfigImpl buildConfig = readConfigFromCache(configHash, hashFile, cacheFile);

        if (buildConfig == null) {
            buildConfig = parseConfig(configData);
            writeConfigCache(configHash, hashFile, cacheFile, buildConfig);
        }

        return buildConfig;
    }

    @SuppressWarnings({"checkstyle:returncount", "checkstyle:illegalcatch"})
    private static BuildConfigImpl readConfigFromCache(final byte[] configHash,
                                                       final Path hashFile, final Path cacheFile)
        throws IOException {

        if (Files.notExists(hashFile) || Files.notExists(cacheFile)) {
            return null;
        }

        final byte[] configHashFromCache = Files.readAllBytes(hashFile);
        if (!Arrays.equals(configHash, configHashFromCache)) {
            return null;
        }

        try (ObjectInputStream out = new ObjectInputStream(
            Files.newInputStream(cacheFile))) {
            return (BuildConfigImpl) out.readObject();
        } catch (final Exception e) {
            LOG.warn(e.getMessage(), e);
            return null;
        }
    }

    private static BuildConfigImpl parseConfig(final byte[] data) {
        final Yaml yaml = new Yaml();
        try (InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(data),
            StandardCharsets.UTF_8)) {
            return yaml.loadAs(in, BuildConfigDTO.class).build();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeConfigCache(final byte[] configHash, final Path hashFile,
                                         final Path cacheFile,
                                         final BuildConfigImpl buildConfig) throws IOException {
        Files.deleteIfExists(hashFile);

        try (ObjectOutputStream out =
                 new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            out.writeObject(buildConfig);
        }

        Files.write(hashFile, configHash);
    }

}
