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

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LoomInstaller {

    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;
    private static final int BUF_SIZE = 8192;

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

    private static String readVersion() {
        final URLClassLoader cl = (URLClassLoader) LoomInstaller.class.getClassLoader();
        final URL resource = cl.findResource("META-INF/MANIFEST.MF");

        if (resource == null) {
            return "{unknown}";
        }

        try (InputStream in = resource.openStream()) {
            final Manifest manifest = new Manifest(in);
            return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path download() throws IOException {
        final String downloadUrl = determineDownloadUrl();
        final Path zipDir = Files.createDirectories(determineBaseDir()
            .resolve(Paths.get("zip", sha1(downloadUrl))));
        final Path downloadFile = zipDir.resolve(extractFilenameFromUrl(downloadUrl));
        downloadZip(new URL(downloadUrl), downloadFile);
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
        if (Files.exists(target)) {
            System.out.println("Skip download of Loom Library from " + url + " - it already "
                + "exists: " + target);
            return;
        }

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
                     StandardOpenOption.CREATE_NEW)) {
                copy(inputStream, out, new ProgressMonitor(totalSize));
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void copy(final InputStream in, final OutputStream out,
                             final ProgressMonitor monitor) throws IOException {

        final byte[] buf = new byte[BUF_SIZE];
        int cnt;
        while ((cnt = in.read(buf)) != -1) {
            out.write(buf, 0, cnt);
            monitor.progress(cnt);
        }
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
        chmod755(loomScript);

        if (Files.notExists(projectRoot.resolve("build.yml"))) {
            System.out.println("Create initial build.yml");
            Files.copy(scriptsRoot.resolve("build.yml"), projectRoot.resolve("build.yml"));
        }
    }

    private static void chmod755(final Path loomScript) throws IOException {
        final PosixFileAttributeView view =
            Files.getFileAttributeView(loomScript, PosixFileAttributeView.class);

        if (view == null) {
            // OS doesn't support POSIX file attributes
            return;
        }

        view.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private static class ProgressMonitor {

        private static final Console CONSOLE = System.console();
        private static final int PCT_100 = 100;

        private final long totalSize;
        private long progress;
        private long lastProgress;

        ProgressMonitor(final long totalSize) {
            this.totalSize = totalSize;
        }

        void progress(final long newProgress) {
            progress += newProgress;
            if (CONSOLE != null) {
                updateProgressIndicator();
            }
        }

        private void updateProgressIndicator() {
            final int pct = (int) (progress * PCT_100 / totalSize);
            if (pct != lastProgress) {
                if (lastProgress != 0) {
                    System.out.print("\r");
                }
                System.out.print("Downloaded " + pct + "%");
                if (pct == PCT_100) {
                    System.out.print("\r");
                }
                lastProgress = pct;
            }
        }

    }

}
