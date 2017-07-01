package jobt.plugin.springboot;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;

public class JarAssembler {

    private static final String SPRING_BOOT_LAUNCHER =
        "org.springframework.boot.loader.JarLauncher";

    private SpringBootPluginSettings pluginSettings;

    public JarAssembler(final SpringBootPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    public void assemble(final Path buildDir, final Path assemblyFile,
                         final String applicationClassname) throws IOException {

        final Manifest manifest = prepareManifest(applicationClassname);
        writeManifest(buildDir, manifest);

        try (final JarOutputStream os = new JarOutputStream(Files.newOutputStream(assemblyFile))) {
            os.setMethod(ZipEntry.STORED);
            os.setLevel(Deflater.NO_COMPRESSION);

            Files.walkFileTree(buildDir, new CreateJarFileVisitor(buildDir, os));
        }
    }

    private Manifest prepareManifest(final String applicationClassname) {
        final Manifest newManifest = new Manifest();
        final Attributes mainAttributes = newManifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttributes.put(new Attributes.Name("Created-By"),
            "Jobt " + System.getProperty("jobt.version"));
        mainAttributes.put(new Attributes.Name("Build-Jdk"),
            String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        mainAttributes.put(new Attributes.Name("Start-Class"), applicationClassname);
        mainAttributes.put(new Attributes.Name("Spring-Boot-Classes"), "BOOT-INF/classes/");
        mainAttributes.put(new Attributes.Name("Spring-Boot-Lib"), "BOOT-INF/lib/");
        mainAttributes.put(new Attributes.Name("Spring-Boot-Version"), pluginSettings.getVersion());

        mainAttributes.put(Attributes.Name.MAIN_CLASS, SPRING_BOOT_LAUNCHER);
        return newManifest;
    }

    private void writeManifest(final Path buildDir, final Manifest manifest) throws IOException {
        final Path manifestDir = Files.createDirectories(buildDir.resolve("META-INF"));
        final Path file = manifestDir.resolve("MANIFEST.MF");
        try (final OutputStream os = new BufferedOutputStream(Files.newOutputStream(file))) {
            manifest.write(os);
        }
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
