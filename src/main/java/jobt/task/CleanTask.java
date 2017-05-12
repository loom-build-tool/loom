package jobt.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import jobt.Progress;
import jobt.plugin.PluginRegistry;

public class CleanTask implements Task {

    public CleanTask(final PluginRegistry pluginRegistry) {
    }

    @Override
    public void run() throws IOException {
        Progress.newStatus("Clean output directory");
        cleanDir("jobtbuild");
        cleanDir(".jobt");
        Progress.complete();
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
