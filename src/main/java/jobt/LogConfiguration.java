package jobt;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

public final class LogConfiguration {

    private LogConfiguration() {
    }

    @SuppressWarnings("checkstyle:executablestatementcount")
    public static void configureLogger() {
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

    public static void stop() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
    }

}
