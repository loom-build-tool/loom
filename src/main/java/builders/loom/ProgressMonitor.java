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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public final class ProgressMonitor {

    private static final int DELAY = 100;
    private static final int JANSI_BUF = 80;
    private static final PrintStream OUT = AnsiConsole.out();

    private static final AtomicInteger tasks = new AtomicInteger();
    private static final AtomicInteger completedTasks = new AtomicInteger();
    private static final AtomicInteger lastProgress = new AtomicInteger();
    private static final Timer timer = new Timer("ProgressMonitor", true);

    private ProgressMonitor() {
    }

    public static void start() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (completedTasks.get() > lastProgress.get()) {
                    update();
                }
            }
        }, DELAY, DELAY);
    }

    public static void setTasks(final int tasks) {
        OUT.println();
        ProgressMonitor.tasks.set(tasks);
    }

    public static void progress() {
        completedTasks.incrementAndGet();
    }

    public static void stop() {
        timer.cancel();
        OUT.print(Ansi.ansi().reset().cursorUp(1).eraseLine());
    }

    private static void update() {
        final int progressBarLength = 25;

        final int cpl = completedTasks.get();
        final int taskCnt = tasks.get();

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

        OUT.println(a);

        lastProgress.set(cpl);
    }

}
