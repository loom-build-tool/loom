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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoomInstaller {

    private static final String PROPERTIES_RESOURCE = "/loom-installer.properties";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;
    private static final int BUF_SIZE = 8192;
    private static final long DOWNLOAD_PROGRESS_INTERVAL = 5_000_000_000L;

    public static void main(final String[] args) {
        try {
            System.out.println("Starting Loom Installer v" + readVersion());

            final Path installDir = extract(download(), determineLibBaseDir());
            copyScripts(installDir, Paths.get(""));
        } catch (final Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String readVersion() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = LoomInstaller.class.getResourceAsStream(PROPERTIES_RESOURCE)) {
            properties.load(in);
            return properties.getProperty("version");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path download() throws IOException {
        final String downloadUrl = determineDownloadUrl();
        final Path zipDir = Files.createDirectories(determineBaseDir()
            .resolve(Paths.get("zip", sha1(downloadUrl))));
        final Path downloadFile = zipDir.resolve(extractFilenameFromUrl(downloadUrl));

        if (Files.exists(downloadFile)) {
            System.out.println("Skip download of Loom Library from " + downloadUrl + " as "
                + "it already exists: " + downloadFile);
        } else {
            downloadZip(new URL(downloadUrl), downloadFile);
        }

        return downloadFile;
    }

    private static String determineDownloadUrl() throws IOException {
        final Path propertiesFile = Paths.get("loom-installer", "loom-installer.properties");

        if (Files.notExists(propertiesFile)) {
            throw new IllegalStateException("Missing configuration of Loom Installer: "
                + propertiesFile);
        }

        final Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            properties.load(in);
        }

        return properties.getProperty("distributionUrl");
    }

    private static String extractFilenameFromUrl(final String downloadUrl) {
        final int idx = downloadUrl.lastIndexOf('/');
        if (idx == -1) {
            throw new IllegalStateException("Cant' parse url: " + downloadUrl);
        }
        return downloadUrl.substring(idx + 1, downloadUrl.length());
    }

    private static String sha1(final String url) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            final byte[] digest = md.digest(url.getBytes(StandardCharsets.UTF_8));
            return encodeHexString(digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static String encodeHexString(final byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static void downloadZip(final URL url, final Path target) throws IOException {
        System.out.println("Downloading Loom Library from " + url + " ...");

        final Path parent = target.getParent();

        if (parent == null) {
            throw new IllegalStateException();
        }

        final Path tmpFile = Files.createTempFile(parent, null, null);

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
                 final OutputStream out = Files.newOutputStream(tmpFile,
                     StandardOpenOption.APPEND)) {
                copy(inputStream, out, totalSize);
            }
        } finally {
            conn.disconnect();
        }

        if (Files.notExists(target)) {
            Files.move(tmpFile, target, StandardCopyOption.ATOMIC_MOVE);
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

    private static Path determineLibBaseDir() throws IOException {
        final Path baseDir = determineBaseDir();
        return Files.createDirectories(baseDir.resolve("library"));
    }

    private static Path determineBaseDir() throws IOException {
        final String loomUserHome = System.getenv("LOOM_USER_HOME");
        if (loomUserHome != null) {
            return Paths.get(loomUserHome);
        }

        if (isWindowsOS()) {
            return determineWindowsBaseDir();
        }

        return determineGenericBaseDir();
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

    private static Path extract(final Path zipFile, final Path dstDir) throws IOException {
        final Path installDir;
        final Path okFile;

        try (final ZipInputStream in = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry nextEntry = in.getNextEntry();

            if (!nextEntry.isDirectory()) {
                throw new IllegalStateException("First entry in " + zipFile
                    + " is not a directory: " + nextEntry);
            }

            installDir = dstDir.resolve(nextEntry.getName());
            okFile = installDir.resolve("loom.ok");

            if (Files.exists(okFile)) {
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
                    Files.copy(in, destFile, StandardCopyOption.REPLACE_EXISTING);
                }

                nextEntry = in.getNextEntry();
            }

            in.closeEntry();
        }

        Files.createFile(okFile);

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

        view.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x"));
    }

}
