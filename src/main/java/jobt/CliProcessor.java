package jobt;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.yaml.snakeyaml.Yaml;

import jobt.config.BuildConfigImpl;

public class CliProcessor {

    private static BuildConfigImpl readConfig() throws IOException {
        final Yaml yaml = new Yaml();

        final Path buildFile = Paths.get("build.yml");
        if (!Files.isRegularFile(buildFile)) {
            throw new IOException("No build.yml found");
        }

        try (final Reader resourceAsStream = Files.newBufferedReader(buildFile,
            StandardCharsets.UTF_8)) {
            return yaml.loadAs(resourceAsStream, BuildConfigImpl.class);
        }
    }

    public void run(final String[] args) throws Exception {
        final long startTime = System.nanoTime();

        Progress.newStatus("Read configuration");
        final BuildConfigImpl buildConfig = readConfig();
        Progress.ok();

        Progress.log("Initialized configuration for %s version %s",
            buildConfig.getProject().getArchivesBaseName(),
            buildConfig.getProject().getVersion());

        final Options options = new Options();
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);

        final TaskTemplateImpl taskTemplate = new TaskTemplateImpl(buildConfig);

        for (final String taskName : cmd.getArgs()) {
            taskTemplate.execute(taskName);
        }

        final double duration = (System.nanoTime() - startTime) / 1_000_000_000D;
        Progress.log(String.format("\uD83C\uDF5D  Cooked your pasta in %.2fs%n", duration));
    }

}
