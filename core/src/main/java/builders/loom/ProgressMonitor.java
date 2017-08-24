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

import java.io.PrintStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public final class ProgressMonitor {

    private static final int DELAY = 100;
    private static final int JANSI_BUF = 100;
    private static final PrintStream OUT = AnsiConsole.out();
    private static final String[] UNITS = {"B", "KiB", "MiB"};

    private static final AtomicInteger TASKS = new AtomicInteger();
    private static final AtomicInteger COMPLETED_TASKS = new AtomicInteger();
    private static final AtomicInteger DOWNLOADED_FILES = new AtomicInteger();
    private static final AtomicLong DOWNLOADED_BYTES = new AtomicLong();
    private static final AtomicLong LAST_PROGRESS = new AtomicLong();
    private static final Timer TIMER = new Timer("ProgressMonitor", true);

    private ProgressMonitor() {
    }

    public static void start() {
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if (COMPLETED_TASKS.get() + DOWNLOADED_BYTES.get() > LAST_PROGRESS.get()) {
                    update();
                }
            }
        }, DELAY, DELAY);
    }

    public static void setTasks(final int tasks) {
        OUT.println();
        ProgressMonitor.TASKS.set(tasks);
    }

    public static void progress() {
        COMPLETED_TASKS.incrementAndGet();
    }

    public static void progressDownloadedFiles() {
        DOWNLOADED_FILES.incrementAndGet();
    }

    public static void progressDownloadedBytes(final long bytes) {
        DOWNLOADED_BYTES.addAndGet(bytes);
    }

    public static void stop() {
        TIMER.cancel();
        OUT.print(Ansi.ansi().reset().cursorUp(1).eraseLine());
    }

    private static void update() {
        final int progressBarLength = 25;

        final int cpl = COMPLETED_TASKS.get();
        final int taskCnt = TASKS.get();

        final int pct = 100 * cpl / taskCnt;
        final int progress = progressBarLength * cpl / taskCnt;
        final int nullProgress = progressBarLength - progress;

        final Ansi a = Ansi.ansi(JANSI_BUF).cursorUp(1)
            .fg(Ansi.Color.WHITE)
            .a('[')
            .a(String.join("", Collections.nCopies(progress, "=")))
            .a('>')
            .a(String.join("", Collections.nCopies(nullProgress, " ")))
            .format("] (%d%%) [%d/%d tasks completed]", pct, cpl, taskCnt);

        if (DOWNLOADED_FILES.intValue() > 0) {
            a.format(" (Downloaded: %d files | %s)",
                DOWNLOADED_FILES.get(), formatBytes(DOWNLOADED_BYTES.get()))
                .eraseLine(Ansi.Erase.FORWARD);
        }

        OUT.println(a);

        LAST_PROGRESS.set(cpl + DOWNLOADED_BYTES.get());
    }

    // not in a util class, because of the special handling (stop at MiB, show floating number)
    @SuppressWarnings("checkstyle:magicnumber")
    private static String formatBytes(final long size) {
        int unit = 0;

        float rsize = size;
        for (; rsize >= 1024 && unit < UNITS.length - 1; unit++) {
            rsize /= 1024.0;
        }

        if (unit == 0) {
            // no floating points for bytes
            return size + " " + UNITS[unit];
        }


        return String.format(Locale.ROOT, "%.2f", rsize) + " " + UNITS[unit];
    }

}
