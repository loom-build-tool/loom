package jobt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.LogManager;

import org.yaml.snakeyaml.Yaml;

import jobt.config.BuildConfigImpl;

public class JobtProcessor {

    private final Stopwatch stopwatch;
    private TaskTemplateImpl taskTemplate;

    public JobtProcessor(final Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
    }

    public BuildConfigImpl readConfig() throws IOException {
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

    public void configureLogger(final boolean enable) {
        final Properties properties = new Properties();
        if (enable) {
            properties.setProperty("handlers", "java.util.logging.ConsoleHandler");
            properties.setProperty("java.util.logging.ConsoleHandler.level", "FINE");
            properties.setProperty("java.util.logging.ConsoleHandler.formatter",
                "java.util.logging.SimpleFormatter");
            properties.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");
        } else {
            properties.setProperty("handlers", "");
        }

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            properties.store(out, null);
            final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            LogManager.getLogManager().readConfiguration(in);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void init(final BuildConfigImpl buildConfig) {
        stopwatch.startProcess("Initialize plugins");
        taskTemplate = new TaskTemplateImpl(buildConfig, stopwatch);
        stopwatch.stopProcess("Initialize plugins");
    }

    public void execute(final String taskName) throws Exception {
        taskTemplate.execute(taskName);
    }

}
