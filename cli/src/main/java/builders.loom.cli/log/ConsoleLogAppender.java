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

package builders.loom.cli.log;

import java.io.IOException;
import java.io.PrintStream;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.event.Level;

public class ConsoleLogAppender implements LogAppender {

    private final PrintStream out = AnsiConsole.out();

    @Override
    public void append(final LogEvent logEvent) {
        if (ignoreLogEvent(logEvent)) {
            return;
        }

        final Ansi ansi = Ansi.ansi();
        switch (logEvent.getLevel()) {
            case ERROR:
                ansi.fgBrightRed().a(logEvent.getLevel().toString()).reset();
                break;
            case WARN:
                ansi.fgBrightYellow().a(logEvent.getLevel().toString()).reset();
                break;
            default:
                ansi.a(logEvent.getLevel().toString());
        }

        ansi
            .a(" [").a(logEvent.getThreadName()).a("] ")
            .fgCyan().a(logEvent.getName()).fgDefault()
            .a(" - ")
            .a(logEvent.getMessage())
            .newline();

        out.println(ansi);
    }

    private boolean ignoreLogEvent(final LogEvent logEvent) {
        if (logEvent.getLevel().toInt() < Level.WARN.toInt()) {
            return true;
        }

        if (logEvent.getMarker() != null && logEvent.getMarker().contains("HIDE_FROM_CONSOLE")) {
            return true;
        }

        return false;
    }

    @Override
    public void close() throws IOException {
    }

}
