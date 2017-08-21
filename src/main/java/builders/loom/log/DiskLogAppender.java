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

package builders.loom.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import builders.loom.misc.ThreadFactoryBuilder;

public class DiskLogAppender implements LogAppender {

    private static final LogEvent POISON_PILL =
        new LogEvent(null, null, null, null, null, null);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss,SSS");
    private static final int SHUTDOWN_TIMEOUT_SECS = 5;

    private final PrintWriter out;
    private final BlockingQueue<LogEvent> queue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService;

    DiskLogAppender(final Path logFile) {
        final Path logDir = logFile.getParent();
        try {
            if (!Files.isDirectory(logDir)) {
                Files.createDirectories(logDir);
            }

            out = new PrintWriter(Files.newBufferedWriter(logFile, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .withDaemon(true)
            .withNameFormat("DiskLogAppender")
            .withPriority(Thread.MIN_PRIORITY)
            .build();
        executorService = Executors.newSingleThreadExecutor(threadFactory);
        executorService.submit(new LogWriter());
    }

    @Override
    public void append(final LogEvent logEvent) {
        queue.add(logEvent);
    }

    @Override
    public void close() throws IOException {
        queue.add(POISON_PILL);

        executorService.shutdown();
        try {
            executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }

        executorService.shutdownNow();

        out.close();
    }

    class LogWriter implements Runnable {

        @Override
        public void run() {
            while (true) {
                final LogEvent logEvent;
                try {
                    logEvent = queue.take();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (logEvent == POISON_PILL) {
                    break;
                }

                out.write(logEvent.getOccurred().format(DATE_TIME_FORMATTER));
                out.write(' ');
                out.write(logEvent.getLevel().toString());
                out.write(" [");
                out.write(logEvent.getThreadName());
                out.write("] ");
                out.write(logEvent.getName());
                out.write(" - ");
                out.println(logEvent.getMessage());

                if (logEvent.getThrowable() != null) {
                    logEvent.getThrowable().printStackTrace(out);
                }
            }
        }

    }

}
