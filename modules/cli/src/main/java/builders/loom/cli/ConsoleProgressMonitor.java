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

package builders.loom.cli;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.core.ProgressMonitor;
import builders.loom.util.StringUtil;

final class ConsoleProgressMonitor implements ProgressMonitor {

    private static final int DELAY = 100;
    private static final int JANSI_BUF = 200;
    private static final PrintStream OUT = AnsiConsole.out();
    private static final String[] UNITS = {"B", "KiB", "MiB"};

    private static final AtomicInteger TASKS = new AtomicInteger();
    private static final AtomicInteger COMPLETED_TASKS = new AtomicInteger();

    private static final AtomicInteger DOWNLOADED_FILES = new AtomicInteger();
    private static final AtomicLong DOWNLOADED_BYTES = new AtomicLong();

    private static final AtomicLong TESTS_TOTAL = new AtomicLong();
    private static final AtomicLong TESTS_SUCCESS = new AtomicLong();
    private static final AtomicLong TESTS_ABORT = new AtomicLong();
    private static final AtomicLong TESTS_SKIP = new AtomicLong();
    private static final AtomicLong TESTS_FAIL = new AtomicLong();
    private static final AtomicLong TESTS_ERROR = new AtomicLong();

    private static final AtomicBoolean UPDATED_REQUIRED = new AtomicBoolean(false);

    private static final Timer TIMER = new Timer("ProgressMonitor", true);

    @Override
    public void start() {
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                if (UPDATED_REQUIRED.getAndSet(false)) {
                    update();
                }
            }
        }, DELAY, DELAY);
    }

    @Override
    public void setTasks(final int tasks) {
        OUT.println();
        ConsoleProgressMonitor.TASKS.set(tasks);
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void progress() {
        COMPLETED_TASKS.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void progressDownloadedFiles() {
        DOWNLOADED_FILES.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void progressDownloadedBytes(final long bytes) {
        DOWNLOADED_BYTES.addAndGet(bytes);
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testsTotal(final long tests) {
        TESTS_TOTAL.set(tests);
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testsAdd() {
        TESTS_TOTAL.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testSuccess() {
        TESTS_SUCCESS.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testAbort() {
        TESTS_ABORT.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testSkip() {
        TESTS_SKIP.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testFail() {
        TESTS_FAIL.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void testError() {
        TESTS_ERROR.incrementAndGet();
        UPDATED_REQUIRED.set(true);
    }

    @Override
    public void stop() {
        TIMER.cancel();
        update();
    }

    private static void update() {
        final int progressBarLength = 25;

        final int cpl = COMPLETED_TASKS.get();
        final int taskCnt = TASKS.get();

        final int pct = 100 * cpl / taskCnt;
        final int progress = progressBarLength * cpl / taskCnt;
        final int nullProgress = progressBarLength - progress;

        final Ansi a = Ansi.ansi(JANSI_BUF).cursorUp(1)
            .fg(Ansi.Color.WHITE);

        if (cpl == taskCnt) {
            a.format("All %d tasks completed ", cpl);
        } else {
            a.a('[')
                .a(String.join("", StringUtil.repeat('=', progress)))
                .a('>')
                .a(String.join("", StringUtil.repeat(' ', nullProgress)))
                .format("] (%d%%) [%d/%d tasks completed]", pct, cpl, taskCnt);
        }

        if (DOWNLOADED_FILES.intValue() > 0) {
            a.format(" (Downloaded: %d files | %s)",
                DOWNLOADED_FILES.get(), formatBytes(DOWNLOADED_BYTES.get()));
        }

        if (TESTS_TOTAL.longValue() > 0) {
            a.render(" (Tests: %d | SU=%d SK=%d A=%d F=%d E=%d)",
                TESTS_TOTAL.get(), TESTS_SUCCESS.get(), TESTS_SKIP.get(), TESTS_ABORT.get(),
                TESTS_FAIL.get(), TESTS_ERROR.get());
        }

        OUT.println(a.eraseLine(Ansi.Erase.FORWARD));
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
