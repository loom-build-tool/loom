/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom;

import java.nio.file.Path;

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
    public static void configureLogger(final Path logFile) {
        final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        lc.start();

        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("%date %level [%thread] %logger - %msg%n");
        encoder.start();

        final PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
        consoleEncoder.setContext(lc);
        consoleEncoder.setPattern("%highlight(%level) [%thread] %cyan(%logger{32}) - %msg%n%n");
        consoleEncoder.start();

        final ThresholdFilter filter = new ThresholdFilter();
        filter.setContext(lc);
        filter.setName("Loom ThresholdFilter");
        filter.setLevel("WARN");
        filter.start();

        final ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(lc);
        consoleAppender.setWithJansi(true);
        consoleAppender.setName("Loom Console Appender");
        consoleAppender.setTarget("System.err");
        consoleAppender.setEncoder(consoleEncoder);
        consoleAppender.addFilter(filter);
        consoleAppender.start();

        final FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(lc);
        fileAppender.setName("Loom File Appender");
        fileAppender.setFile(logFile.toAbsolutePath().toString());
        fileAppender.setAppend(false);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        final Logger loomLogger = lc.getLogger("builders.loom");
        loomLogger.setAdditive(false);
        loomLogger.setLevel(Level.DEBUG);
        loomLogger.detachAppender("console");
        loomLogger.addAppender(consoleAppender);
        loomLogger.addAppender(fileAppender);

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
