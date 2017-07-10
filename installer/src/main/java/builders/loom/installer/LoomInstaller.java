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

package builders.loom.installer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoomInstaller {

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;
    private static final int BUF_SIZE = 8192;
    private static final long DOWNLOAD_PROGRESS_INTERVAL = 5_000_000_000L;

    public static void main(final String[] args) {
        Path tmpFile = null;

        try {
            System.out.println("Starting Loom Installer v" + readVersion());

            // Read config
            final URL downloadUrl = determineDownloadUrl();

            // Download
            tmpFile = Files.createTempFile("loom", null);
            downloadZip(downloadUrl, tmpFile);

            // Extract
            final Path rootDirectory = extractZip(tmpFile, determineTargetDir());

            // Copy scripts
            copyScripts(rootDirectory, Paths.get(""));
        } catch (final Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            // Cleanup
            cleanup(tmpFile);
        }
    }

    private static String readVersion() {
        final ClassLoader cl = LoomInstaller.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream("META-INF/MANIFEST.MF")) {
            final Manifest manifest = new Manifest(in);
            return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL determineDownloadUrl() throws IOException {
        final Path propertiesFile = Paths.get("loom-installer", "loom-installer.properties");

        if (Files.notExists(propertiesFile)) {
            throw new IllegalStateException("Missing configuration of Loom Installer: "
                + propertiesFile);
        }

        final Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            properties.load(in);
        }

        return new URL(properties.getProperty("distributionUrl"));
    }

    private static void downloadZip(final URL url, final Path target) throws IOException {
        System.out.println("Downloading Loom Library from " + url + " ...");

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Connecting " + url + " resulted in "
                    + conn.getHeaderField(0));
            }

            final long totalSize = conn.getContentLengthLong();

            try (final InputStream inputStream = conn.getInputStream();
                 final OutputStream out = Files.newOutputStream(target,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
                copy(inputStream, out, totalSize);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void copy(final InputStream in, final OutputStream out,
                             final long totalSize) throws IOException {

        long start = System.nanoTime();

        final byte[] buf = new byte[BUF_SIZE];
        int cnt;
        int transferred = 0;
        boolean progressShown = false;
        while ((cnt = in.read(buf)) != -1) {
            out.write(buf, 0, cnt);
            transferred += cnt;
            if (System.nanoTime() - start > DOWNLOAD_PROGRESS_INTERVAL) {
                showProgress(totalSize, transferred);
                start = System.nanoTime();
                progressShown = true;
            }
        }

        if (progressShown) {
            showProgress(totalSize, transferred);
        }
    }

    private static void showProgress(final long totalSize, final int transferred) {
        final int pct = (int) (transferred * 100.0 / totalSize);
        System.out.println("Downloaded " + pct + " %");
    }

    private static Path determineTargetDir() throws IOException {
        final Path baseDir;

        final String loomUserHome = System.getenv("LOOM_USER_HOME");
        if (loomUserHome != null) {
            baseDir = Paths.get(loomUserHome);
        } else if (isWindowsOS()) {
            baseDir = determineWindowsBaseDir();
        } else {
            baseDir = determineGenericBaseDir();
        }

        return Files.createDirectories(baseDir.resolve("binary"));
    }

    private static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return osName != null && osName.startsWith("Windows");
    }

    private static Path determineWindowsBaseDir() throws IOException {
        final String localAppDataEnv = System.getenv("LOCALAPPDATA");

        if (localAppDataEnv == null) {
            throw new IllegalStateException("Windows environment variable LOCALAPPDATA missing");
        }

        final Path localAppDataDir = Paths.get(localAppDataEnv);

        if (!Files.isDirectory(localAppDataDir)) {
            throw new IllegalStateException("Windows environment variable LOCALAPPDATA points to "
                + "a non existing directory: " + localAppDataDir);
        }

        return localAppDataDir.resolve(Paths.get("Loom", "Loom"));
    }

    private static Path determineGenericBaseDir() throws IOException {
        final String userHomeVar = System.getProperty("user.home");
        final Path userHome = Paths.get(userHomeVar);

        if (!Files.isDirectory(userHome)) {
            throw new IllegalStateException("User home (" + userHomeVar + ") doesn't exist");
        }

        return userHome.resolve(".loom");
    }

    private static Path extractZip(final Path zipFile, final Path dstDir) throws IOException {
        final Path installDir;

        try (final ZipInputStream in = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry nextEntry = in.getNextEntry();

            if (!nextEntry.isDirectory()) {
                throw new IllegalStateException("First entry in " + zipFile
                    + " is not a directory: " + nextEntry);
            }

            installDir = dstDir.resolve(nextEntry.getName());

            if (Files.exists(installDir)) {
                System.out.println("Skip installation to " + installDir + " as it exists already");
                return installDir;
            }

            System.out.println("Install Loom Library to " + installDir + " ...");

            while (nextEntry != null) {
                final String entryName = nextEntry.getName();
                Objects.requireNonNull(entryName, "entryName must not be null");

                final Path destFile = dstDir.resolve(entryName);

                if (nextEntry.isDirectory()) {
                    Files.createDirectories(destFile);
                } else {
                    Files.copy(in, destFile);
                }

                nextEntry = in.getNextEntry();
            }

            in.closeEntry();
        }

        return installDir;
    }

    private static void copyScripts(final Path loomRootDirectory, final Path projectRoot)
        throws IOException {

        System.out.println("Create Loom build scripts");
        final Path scriptsRoot = loomRootDirectory.resolve("scripts");

        Files.copy(scriptsRoot.resolve("loom.cmd"), projectRoot.resolve("loom.cmd"),
            StandardCopyOption.REPLACE_EXISTING);

        final Path loomScript = projectRoot.resolve("loom");
        Files.copy(scriptsRoot.resolve("loom"), loomScript,
            StandardCopyOption.REPLACE_EXISTING);
        chmod(loomScript, "rwxr-xr-x");

        if (Files.notExists(projectRoot.resolve("build.yml"))) {
            System.out.println("Create initial build.yml");
            Files.copy(scriptsRoot.resolve("build.yml"), projectRoot.resolve("build.yml"));
        }
    }

    private static void chmod(final Path file, final String perms) throws IOException {
        final PosixFileAttributeView view =
            Files.getFileAttributeView(file, PosixFileAttributeView.class);

        if (view == null) {
            // OS (Windows) doesn't support POSIX file attributes
            return;
        }

        view.setPermissions(PosixFilePermissions.fromString(perms));
    }

    private static void cleanup(final Path tmpFile) {
        try {
            if (tmpFile != null) {
                Files.deleteIfExists(tmpFile);
            }
        } catch (final IOException ignore) {
            // ignore
        }
    }

}
