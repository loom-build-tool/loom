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

package builders.loom.plugin.springboot;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

public class JarAssembler {

    public void assemble(final Path buildDir, final Path jarFile) throws IOException {
        final Manifest manifest = readManifest(buildDir);

        try (final JarOutputStream os = new JarOutputStream(Files.newOutputStream(jarFile),
            manifest)) {

            os.setMethod(ZipEntry.STORED);
            os.setLevel(Deflater.NO_COMPRESSION);

            Files.walkFileTree(buildDir, new CreateJarFileVisitor(buildDir, os));
        }
    }

    private Manifest readManifest(final Path buildDir) throws IOException {
        final Path manifestFile = buildDir.resolve(Paths.get("META-INF", "MANIFEST.MF"));
        final Manifest manifest = new Manifest();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(manifestFile))) {
            manifest.read(in);
        }
        return manifest;
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static long crc32(final Path file) throws IOException {
        final byte[] buffer = new byte[8192];
        int bytesRead;

        final CRC32 crc = new CRC32();

        try (InputStream in = Files.newInputStream(file)) {
            while ((bytesRead = in.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }

        return crc.getValue();
    }

    private static class CreateJarFileVisitor extends SimpleFileVisitor<Path> {

        private final Path sourceDir;
        private final JarOutputStream os;

        CreateJarFileVisitor(final Path sourceDir, final JarOutputStream os) {
            this.sourceDir = sourceDir;
            this.os = os;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                                                 final BasicFileAttributes attrs)
            throws IOException {

            if (dir.equals(sourceDir)) {
                return FileVisitResult.CONTINUE;
            }

            final Path dirName = dir.getFileName();
            if (dirName != null && "META-INF".equals(dirName.toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }

            final JarEntry entry = new JarEntry(sourceDir.relativize(dir).toString() + "/");
            entry.setTime(attrs.lastModifiedTime().toMillis());
            entry.setSize(0);
            entry.setCrc(0);
            os.putNextEntry(entry);
            os.closeEntry();

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs)
            throws IOException {

            final JarEntry entry = new JarEntry(sourceDir.relativize(file).toString());
            entry.setTime(attrs.lastModifiedTime().toMillis());
            entry.setSize(attrs.size());
            entry.setCrc(crc32(file));
            os.putNextEntry(entry);
            Files.copy(file, os);
            os.closeEntry();
            return FileVisitResult.CONTINUE;
        }

    }

}
