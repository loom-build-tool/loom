package jobt.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import jobt.Progress;
import jobt.config.BuildConfig;

public class JavaAssemblePlugin extends AbstractPlugin {

    private final BuildConfig buildConfig;

    public JavaAssemblePlugin(final BuildConfig buildConfig) {
        super("assemble");
        this.buildConfig = buildConfig;
    }

    @Override
    public void run() throws Exception {
        Progress.newStatus("Assemble jar");

        final Path buildDir = Paths.get("jobtbuild/libs");
        Files.createDirectories(buildDir);

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        final String jarName = String.format("%s-%s.jar",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion());

        try (final JarOutputStream os = new JarOutputStream(
            Files.newOutputStream(buildDir.resolve(jarName)), manifest)) {
            final Path baseDir = Paths.get("jobtbuild", "build");

            final List<Path> paths = Files.walk(baseDir)
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            for (final Path path : paths) {
                final String fileName = baseDir.relativize(path).toString();
                if (!fileName.isEmpty()) {
                    add(fileName, path, os);
                }
            }
        }

        Progress.complete();
    }

    private static void add(final String name, final Path source, final JarOutputStream target)
        throws IOException {

        final JarEntry entry;

        if (Files.isDirectory(source)) {
            entry = new JarEntry(name + "/");
        } else if (Files.isRegularFile(source)) {
            entry = new JarEntry(name);
        } else {
            throw new IllegalStateException();
        }

        entry.setTime(Files.getLastModifiedTime(source).toMillis());
        target.putNextEntry(entry);

        if (Files.isRegularFile(source)) {
            Files.copy(source, target);
        }

        target.closeEntry();
    }

}
