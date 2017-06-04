package jobt.plugin.java;

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

import jobt.api.BuildConfig;
import jobt.api.Task;
import jobt.api.TaskStatus;

public class JavaAssembleTask implements Task {

    private final BuildConfig buildConfig;

    public JavaAssembleTask(final BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }

    @Override
    public void prepare() throws Exception {
    }

    @Override
    public TaskStatus run() throws Exception {
        final Path buildDir = Paths.get("jobtbuild/libs");
        Files.createDirectories(buildDir);

        final Manifest manifest = new Manifest();
        final Attributes mainAttributes = manifest.getMainAttributes();
        mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        final String mainClassName = buildConfig.getConfiguration().get("mainClassName");
        if (mainClassName != null) {
            mainAttributes.put(Attributes.Name.MAIN_CLASS, mainClassName);
        }

        final String jarName = String.format("%s-%s.jar",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion());

        try (final JarOutputStream os = new JarOutputStream(
            Files.newOutputStream(buildDir.resolve(jarName)), manifest)) {
            final Path baseDir = JavaCompileTask.BUILD_MAIN_PATH;

            final List<Path> paths = Files.walk(baseDir)
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            for (final Path path : paths) {
                final String fileName = baseDir.relativize(path).toString();
                if (!fileName.isEmpty()) {
                    add(fileName, path, os);
                }
            }
        }

        return TaskStatus.OK;
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
