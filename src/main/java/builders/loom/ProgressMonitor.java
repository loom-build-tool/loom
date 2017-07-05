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

    private static AtomicInteger tasks = new AtomicInteger();
    private static AtomicInteger completedTasks = new AtomicInteger();
    private static AtomicInteger lastProgress = new AtomicInteger();
    private static Timer timer = new Timer("ProgressMonitor", true);

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
        OUT.print(Ansi.ansi().reset().cursorUpLine().eraseLine());
    }

    private static void update() {
        final int progressBarLength = 25;

        final int cpl = completedTasks.get();
        final int taskCnt = tasks.get();

        final int pct = 100 * cpl / taskCnt;
        final int progress = progressBarLength * cpl / taskCnt;
        final int nullProgress = progressBarLength - progress;

        final Ansi a = Ansi.ansi(JANSI_BUF).cursorUpLine().cursorToColumn(0)
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
