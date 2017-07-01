package jobt.plugin.java;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetBasePath;
    private final KeyValueCache cache;
    private final PathMatcher matcher;
    private final Map<String, String> resourceFilterVariables;
    private Path sourceBasePath;

    CopyFileVisitor(final Path targetBasePath, final KeyValueCache cache,
                    final String filterGlob,
                    final Map<String, String> resourceFilterVariables) throws IOException {

        this.targetBasePath = targetBasePath;
        this.cache = cache;
        this.resourceFilterVariables = resourceFilterVariables;
        Files.createDirectories(targetBasePath);

        matcher = filterGlob != null
            ? FileSystems.getDefault().getPathMatcher("glob:" + filterGlob)
            : null;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
        throws IOException {

        if (sourceBasePath == null) {
            sourceBasePath = dir;
        } else {
            final Path destPath = targetBasePath.resolve(sourceBasePath.relativize(dir));
            if (Files.exists(destPath) && !Files.isDirectory(destPath)) {
                Files.delete(destPath);
            }

            Files.createDirectory(destPath);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
        throws IOException {

        final long lastModifiedTime = Files.getLastModifiedTime(file).toMillis();

        final Path relativizedFile = sourceBasePath.relativize(file);

        final String cacheKey = relativizedFile.toString();
        final Long cachedMtime = cache.get(cacheKey);
        if (cachedMtime != null && cachedMtime == lastModifiedTime) {
            return FileVisitResult.CONTINUE;
        }

        final Path destPath = targetBasePath.resolve(relativizedFile);

        if (Files.exists(destPath) && Files.isDirectory(destPath)) {
            FileUtil.deleteDirectoryRecursively(destPath, true);
        }

        if (matcher.matches(relativizedFile)) {
            try (final ResourceFilteringOutputStream out = newOut(destPath)) {
                Files.copy(file, out);
            }
        } else {
            Files.copy(file, destPath, StandardCopyOption.REPLACE_EXISTING);
        }

        cache.put(cacheKey, lastModifiedTime);

        return FileVisitResult.CONTINUE;
    }

    private ResourceFilteringOutputStream newOut(final Path destPath) throws IOException {
        return new ResourceFilteringOutputStream(new BufferedOutputStream(Files.newOutputStream(
            destPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)),
            resourceFilterVariables);
    }

}
