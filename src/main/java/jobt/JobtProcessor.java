package jobt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutionException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import jobt.config.BuildConfigImpl;

@SuppressWarnings("checkstyle:classdataabstractioncoupling")
public class JobtProcessor {

    private final Stopwatch stopwatch;
    private TaskTemplateImpl taskTemplate;

    public JobtProcessor(final Stopwatch stopwatch) {
        this.stopwatch = stopwatch;
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
        fileAppender.setFile(".jobt/build.log");
        fileAppender.setAppend(false);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        final Logger jobtLogger = lc.getLogger("jobt");
        jobtLogger.setAdditive(false);
        jobtLogger.setLevel(Level.DEBUG);
        jobtLogger.detachAppender("console");
        jobtLogger.addAppender(consoleAppender);
        jobtLogger.addAppender(fileAppender);

        final Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setAdditive(false);
        rootLogger.setLevel(Level.ERROR);
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

    public void clean() throws ExecutionException, InterruptedException {
        cleanDir(Paths.get("jobtbuild"));
        cleanDir(Paths.get(".jobt"));
    }

    private static void cleanDir(final Path rootPath) {
        if (!Files.isDirectory(rootPath)) {
            return;
        }

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc)
                    throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
