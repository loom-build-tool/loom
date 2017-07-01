package jobt;

import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class ProgressMonitor {

    private static AtomicInteger tasks = new AtomicInteger();
    private static AtomicInteger completedTasks = new AtomicInteger();
    private static AtomicInteger lastProgress = new AtomicInteger();
    private static Timer timer = new Timer("ProgressMonitor", true);

    public static void start() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (completedTasks.get() > lastProgress.get()) {
                    update();
                }
            }
        }, 100, 100);
    }

    public static void setTasks(final int tasks) {
        AnsiConsole.out().print(Ansi.ansi().reset().saveCursorPosition());

        ProgressMonitor.tasks.set(tasks);
    }

    public static void progress() {
        completedTasks.incrementAndGet();
    }

    public static void stop() {
        timer.cancel();
        AnsiConsole.out().print(Ansi.ansi().reset().restoreCursorPosition().eraseLine());
    }

    private static synchronized void update() {
        final int cpl = completedTasks.get();

        final int taskCnt = tasks.get();
        final double pct = 100.0 / taskCnt * cpl;

        int progressBarLength = 25;
        int progress = (int) Math.floor(((double) progressBarLength) / taskCnt * cpl);
        int nullProgress = progressBarLength - progress;

        final Ansi a = Ansi.ansi(100).restoreCursorPosition().fg(Ansi.Color.WHITE)
            .a('[')
            .a(String.join("", Collections.nCopies(progress, "=")))
            .a('>')
            .a(String.join("", Collections.nCopies(nullProgress, " ")))
            .format("] (%d) [%d/%d tasks completed]", (int) pct, cpl, taskCnt);

        AnsiConsole.out().print(a);
        AnsiConsole.out().flush();

        lastProgress.set(cpl);
    }

}
