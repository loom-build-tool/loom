package jobt.plugin.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import jobt.api.Task;
import jobt.api.TaskStatus;

public class CleanTask implements Task {

    @Override
    public TaskStatus run() throws IOException {
        final Path jobtbuild = Paths.get("jobtbuild");
        final Path jobtconfig = Paths.get(".jobt");

        int cleans = 0;
        if (Files.isDirectory(jobtbuild)) {
            cleanDir(jobtbuild);
            cleans++;
        }

        if (Files.isDirectory(jobtconfig)) {
            cleanDir(jobtconfig);
            cleans++;
        }

        return cleans > 0 ? TaskStatus.OK : TaskStatus.UP_TO_DATE;
    }

    private void cleanDir(final Path rootPath) throws IOException {
        Files.walk(rootPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

}
