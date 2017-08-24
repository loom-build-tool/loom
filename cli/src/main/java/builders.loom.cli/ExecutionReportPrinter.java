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

import java.util.Collections;
import java.util.Map;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import builders.loom.core.ExecutionReport;
import builders.loom.core.ExecutionStatus;
import builders.loom.core.plugin.TaskType;

final class ExecutionReportPrinter {

    void print(final ExecutionReport executionReport) {
        final Map<String, ExecutionStatus> durations = executionReport.getDurations();

        final int longestKey = durations.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(0);

        final long totalDuration = durations.values().stream()
            .mapToLong(ExecutionStatus::getDuration)
            .sum();

        AnsiConsole.out().println(Ansi.ansi()
            .newline()
            .bold()
            .a("Execution statistics (ordered by product completion time):")
            .reset()
            .newline());

        durations.forEach((key, value) -> printDuration(longestKey, totalDuration, key, value));
    }

    private static void printDuration(final int longestKey, final long totalDuration,
                                      final String name, final ExecutionStatus executionStatus) {
        final double pct = 100D / totalDuration * executionStatus.getDuration();
        final String space = String.join("",
            Collections.nCopies(longestKey - name.length(), " "));

        final double minDuration = 0.1;
        final String durationBar = pct < minDuration ? "." : String.join("",
            Collections.nCopies((int) Math.ceil(pct / 2), "#"));

        final double durationSecs = executionStatus.getDuration() / 1_000_000_000D;

        final Ansi a = Ansi.ansi();

        if (executionStatus.getType() == TaskType.GOAL) {
            a.fgBrightYellow().a("Goal");
        } else {
            a.fgBrightCyan().a("Task");
        }

        a.reset()
            .a(' ')
            .a(name)
            .a(' ')
            .a(space)
            .format(" %5.2fs", durationSecs)
            .format(" (%4.1f%%)", pct)
            .a(' ')
            .a(durationBar);

        AnsiConsole.out().println(a);
    }

}
