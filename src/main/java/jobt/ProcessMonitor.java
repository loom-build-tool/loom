package jobt;

import com.google.common.base.Strings;

public class ProcessMonitor {

    private final Thread thread;
    final MyRunnable runnable;


    public ProcessMonitor() {
        runnable = new MyRunnable();

        thread = new Thread(runnable);
        thread.start();
    }

    public void updateProcess(final String message) {
        runnable.updateMessage(message);
    }

    public void endProcess(final String message) {
        runnable.endOldMessage(message);
    }

    public void newProcess(final String message, final String newMessage) {
        runnable.endOldMessage(message);
        runnable.updateMessage(message);
    }

    public void stop() {
        thread.interrupt();
        runnable.endOldMessage();
    }

    private static class MyRunnable implements Runnable {

        private String currentMessage;

        private final String[] icons = {
            "\uD83D\uDD5C",
            "\uD83D\uDD5D",
            "\uD83D\uDD5E",
            "\uD83D\uDD5F",
            "\uD83D\uDD60",
            "\uD83D\uDD61",
            "\uD83D\uDD62",
            "\uD83D\uDD63",
            "\uD83D\uDD64",
            "\uD83D\uDD65",
            "\uD83D\uDD66",
            "\uD83D\uDD67"
        };

        int i = 0;

        @Override
        public void run() {
            while (true) {

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                updateProcess();
            }
        }

        private void updateProcess() {
            consoleOut("\r" + icons[i]);
            i++;
            if (i > icons.length - 1) {
                i = 0;
            }
        }

        public void updateMessage(final String message) {
            endOldMessage();
            currentMessage = message;
            consoleOut("\r" + icons[i] + " " + currentMessage);
        }

        public void endOldMessage() {
            consoleOut("\r" + icons[i] + " " + currentMessage);
        }

        public void endOldMessage(final String message) {
            consoleOut("\r" + icons[i] + " " + message);
            if (message.length() < currentMessage.length()) {
                consoleOut(Strings.repeat(" ", currentMessage.length() - message.length()));
            }
            consoleOut("\n");
        }

        private void consoleOut(final String output) {
            System.out.print(output);
        }


    }
}
