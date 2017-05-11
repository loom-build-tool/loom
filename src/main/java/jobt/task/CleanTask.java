package jobt.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import jobt.plugin.PluginRegistry;

public class CleanTask implements Task {

    public CleanTask(final PluginRegistry pluginRegistry) {
    }

    @Override
    public void run() throws IOException {
        System.out.println("Clean output directory");
        final Path rootPath = Paths.get("jobtbuild");
        if (Files.isDirectory(rootPath)) {
            Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

}
