package jobt;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
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

    @SuppressWarnings("checkstyle:executablestatementcount")
    public void configureLogger() {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.start();

        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("%date %level [%thread] %logger - %msg%n");
        encoder.start();

        final PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(lc);
        consoleEncoder.setPattern("%level %logger - %msg%n");
        consoleEncoder.start();

        final ThresholdFilter filter = new ThresholdFilter();
        filter.setContext(lc);
        filter.setName("Jobt ThresholdFilter");
        filter.setLevel("WARN");
        filter.start();

        final ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(lc);
        consoleAppender.setName("Jobt Console Appender");
        consoleAppender.setTarget("System.err");
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.addFilter(filter);
        consoleAppender.start();

        final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(lc);
        fileAppender.setName("Jobt File Appender");
        fileAppender.setFile("jobtbuild/build.log");
        fileAppender.setAppend(false);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        final Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.setLevel(Level.INFO);
        rootLogger.detachAppender("console");
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(fileAppender);
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
