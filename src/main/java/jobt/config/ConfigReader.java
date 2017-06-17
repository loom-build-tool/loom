package jobt.config;

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

import jobt.Hasher;
import jobt.RuntimeConfigurationImpl;

public final class ConfigReader {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);

    private ConfigReader() {
    }

    public static BuildConfigImpl readConfig(final RuntimeConfigurationImpl runtimeConfiguration)
        throws IOException {
        final Path buildFile = Paths.get("build.yml");

        if (!Files.isRegularFile(buildFile)) {
            throw new IOException("No build.yml found");
        }

        final byte[] configData = Files.readAllBytes(buildFile);

        if (!runtimeConfiguration.isCacheEnabled()) {
            return parseConfig(configData);
        }

        return parseAndCacheConfig(configData);
    }

    private static BuildConfigImpl parseAndCacheConfig(final byte[] configData) throws IOException {
        final byte[] configHash = Hasher.hash(configData);

        final Path cachePath = Paths.get(".jobt", "cache", "base");

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
            return yaml.loadAs(in, BuildConfigImpl.class);
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
