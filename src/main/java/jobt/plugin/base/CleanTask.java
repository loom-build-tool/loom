package jobt.plugin.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import jobt.plugin.Task;

public class CleanTask implements Task {

    @Override
    public void run() throws IOException {
        cleanDir("jobtbuild");
        cleanDir(".jobt");
    }

    private void cleanDir(final String path) throws IOException {
        final Path rootPath = Paths.get(path);
        if (Files.isDirectory(rootPath)) {
            Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

}
